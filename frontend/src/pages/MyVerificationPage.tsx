import { useNavigate } from 'react-router-dom'
import MaterialIcon from '../components/icons/MaterialIcon'
import TopBar from '../components/TopBar'
import { DOCUMENT_TYPE_LABEL, PRIORITY_OPTIONS } from '../data/priorityVerification'
import { useMyInfo } from '../hooks/useMyInfo'
import { useMyVerifications } from '../hooks/useMyVerifications'
import { formatJoinedPeriod, nicknameInitial } from '../lib/profile'
import type { MyVerification, VerificationStatus } from '../types/verification'

const REVIEW_STEPS = ['서류 접수', '검토 중', '완료'] as const

const STATUS_LABEL: Record<VerificationStatus, string> = {
  PENDING: '검토 중',
  APPROVED: '인증 완료',
  REJECTED: '다시 제출 필요',
  EXPIRED: '기간 만료',
}

/** 배지 색 — 반려·만료는 성공처럼 보이지 않게 회색으로 둔다 */
const STATUS_BADGE: Record<VerificationStatus, string> = {
  PENDING: 'bg-amber-badge text-amber-badge-ink',
  APPROVED: 'bg-primary-tint text-primary-tint-ink',
  REJECTED: 'bg-sunken text-ink-2',
  EXPIRED: 'bg-sunken text-ink-2',
}

const STATUS_ICON: Record<VerificationStatus, string> = {
  PENDING: 'hourglass_top',
  APPROVED: 'verified',
  REJECTED: 'error',
  EXPIRED: 'error',
}

function formatMonthDay(isoDateTime: string) {
  const date = new Date(isoDateTime)
  return `${date.getMonth() + 1}.${date.getDate()}`
}

// 반려·만료된 뒤에는 같은 유형으로 다시 신청할 수 있다 (심사 중·승인 상태면 서버가 409로 거절)
function canReapply(status: VerificationStatus) {
  return status === 'REJECTED' || status === 'EXPIRED'
}

/** 상태별 안내 문구. 반려 사유·만료일은 서버가 해당 상태일 때만 내려준다. */
function statusNote({ status, rejectionReason, expiresAt }: MyVerification) {
  if (status === 'PENDING') return '보통 1~2일(평일 기준) 안에 끝나요. 결과는 마이페이지에서 확인할 수 있어요.'
  if (status === 'APPROVED') return expiresAt ? `${formatMonthDay(expiresAt)}까지 유효해요.` : '인증이 완료됐어요.'
  if (status === 'REJECTED') return rejectionReason ?? '서류를 확인하지 못했어요. 다시 신청해 주세요.'
  return '인증 기간이 끝났어요. 다시 신청해 주세요.'
}

/** 신입생 인증(API의 FRESHMAN) 신청 건의 진행 상태 */
function FreshmanCard({ verification }: { verification: MyVerification }) {
  const navigate = useNavigate()
  const { status, submittedAt, documentType } = verification
  const approved = status === 'APPROVED'
  // 3단계 진행 바에서 어디까지 채울지 — 승인이면 끝까지, 심사 중이면 '검토 중'까지
  const done = approved ? 3 : 2
  // 반려·만료는 진행 중인 심사가 없어서 진행 바를 보여주지 않는다
  const reapply = canReapply(status)

  return (
    <div className="mb-2.5 rounded-[18px] border border-border bg-surface px-4 py-4">
      <div className="flex items-center gap-2.5">
        <span className="flex size-[38px] flex-none items-center justify-center rounded-xl bg-amber-tint text-amber-ink">
          <MaterialIcon name={STATUS_ICON[status]} size={20} />
        </span>
        <div className="min-w-0 flex-1">
          <div className="text-[15px] font-bold text-label">{PRIORITY_OPTIONS.freshman.title}</div>
          <div className="mt-0.5 text-[12px] font-medium text-label-alt">
            {formatMonthDay(submittedAt)} 신청{documentType && ` · ${DOCUMENT_TYPE_LABEL[documentType]}`}
          </div>
        </div>
        <span className={`flex-none rounded-[7px] px-2 py-1 text-[11px] font-bold ${STATUS_BADGE[status]}`}>
          {STATUS_LABEL[status]}
        </span>
      </div>

      {!reapply && (
        <>
          <div className="mt-3.5 flex gap-1.5">
            {REVIEW_STEPS.map((_, i) => (
              <span
                key={i}
                className={`h-[5px] flex-1 rounded-[3px] ${
                  i < done - 1 || (i === done - 1 && approved)
                    ? 'bg-primary'
                    : i === done - 1
                      ? 'bg-amber-step'
                      : 'bg-track'
                }`}
              />
            ))}
          </div>
          <div className="mt-1.5 flex">
            {REVIEW_STEPS.map((label, i) => (
              <span
                key={label}
                className={`flex-1 text-[11px] font-bold ${
                  i === 0 ? 'text-left' : i === REVIEW_STEPS.length - 1 ? 'text-right' : 'text-center'
                } ${i < done ? 'text-accent' : 'font-medium text-label-alt'}`}
              >
                {label}
              </span>
            ))}
          </div>
        </>
      )}

      <p className="mt-2.5 text-[12px] leading-normal font-medium text-label-alt">{statusNote(verification)}</p>

      {reapply && (
        <button
          type="button"
          onClick={() => navigate('/verify/priority/freshman')}
          className="mt-3 w-full cursor-pointer rounded-xl border border-border bg-surface py-2.5 text-[13px] font-bold text-ink-2"
        >
          다시 신청하기
        </button>
      )}
    </div>
  )
}

/** 신입생 인증을 신청한 적이 없을 때 */
function FreshmanRow() {
  const navigate = useNavigate()
  const { title, description, icon } = PRIORITY_OPTIONS.freshman
  return (
    <div className="mb-2.5 flex items-center gap-2.5 rounded-[18px] border border-border bg-surface px-4 py-4">
      <span className="flex size-[38px] flex-none items-center justify-center rounded-xl bg-sunken text-label-alt">
        <MaterialIcon name={icon} size={20} />
      </span>
      <div className="min-w-0 flex-1">
        <div className="text-[15px] font-bold text-label">{title}</div>
        <div className="mt-0.5 text-[12px] font-medium text-label-alt">{description}</div>
      </div>
      <button
        type="button"
        onClick={() => navigate('/verify/priority/freshman')}
        className="flex-none cursor-pointer text-[13px] font-bold text-accent"
      >
        추가
      </button>
    </div>
  )
}

/** GPS 동네 인증(API의 NEIGHBORHOOD). 유효하게 승인돼 있으면 완료, 아니면 인증하러 가는 행 */
function NeighborhoodRow({ verification }: { verification?: MyVerification }) {
  const navigate = useNavigate()
  const verified = verification?.status === 'APPROVED'
  return (
    <div className="flex items-center gap-2.5 rounded-[18px] border border-border bg-surface px-4 py-4">
      <span
        className={`flex size-[38px] flex-none items-center justify-center rounded-xl ${
          verified ? 'bg-primary-tint text-accent' : 'bg-sunken text-label-alt'
        }`}
      >
        <MaterialIcon name="location_on" size={20} />
      </span>
      <div className="min-w-0 flex-1">
        <div className="text-[15px] font-bold text-label">동네 인증 · 월계1동</div>
        <div className="mt-0.5 text-[12px] font-medium text-label-alt">
          {verified && verification
            ? `${formatMonthDay(verification.submittedAt)} GPS 인증`
            : '나눔을 신청하려면 동네 인증이 필요해요'}
        </div>
      </div>
      {verified ? (
        <span className="flex-none rounded-[7px] bg-primary px-2 py-1 text-[11px] font-bold text-screen">완료</span>
      ) : (
        <button
          type="button"
          onClick={() => navigate('/verify/location')}
          className="flex-none cursor-pointer text-[13px] font-bold text-accent"
        >
          {verification ? '다시 인증' : '인증하기'}
        </button>
      )}
    </div>
  )
}

/** 기초수급자 인증(API의 LOW_INCOME). 본인 화면에만 보이는 상태다. */
function LowIncomeCard({ verification }: { verification?: MyVerification }) {
  const navigate = useNavigate()
  const status = verification?.status
  return (
    <div className="rounded-[18px] bg-primary-tint px-4 py-4">
      <div className="flex items-center gap-2.5">
        <span className="flex size-[38px] flex-none items-center justify-center rounded-xl bg-surface text-accent">
          <MaterialIcon name="volunteer_activism" size={20} />
        </span>
        <div className="min-w-0 flex-1">
          <div className="text-[15px] font-bold text-primary-tint-ink">{PRIORITY_OPTIONS.basic.title}</div>
          <div className="mt-0.5 text-[12px] font-medium text-primary-tint-ink opacity-80">대기열 가산점이 적용돼요</div>
        </div>
        <span className="flex-none rounded-[7px] bg-surface px-2 py-1 text-[11px] font-bold text-body">
          {status ? STATUS_LABEL[status] : '선택'}
        </span>
      </div>
      <div className="mt-3 flex flex-wrap gap-1.5">
        {PRIORITY_OPTIONS.basic.docTypes.map((doc) => (
          <span key={doc} className="rounded-[7px] bg-surface px-2 py-1 text-[11px] font-semibold text-primary-tint-ink">
            {doc}
          </span>
        ))}
      </div>
      {verification && (
        <p className="mt-3 text-[12px] leading-normal font-medium text-primary-tint-ink">{statusNote(verification)}</p>
      )}
      {(!status || canReapply(status)) && (
        <button
          type="button"
          onClick={() => navigate('/verify/priority/basic')}
          className="mt-3.5 flex h-[46px] w-full cursor-pointer items-center justify-center gap-1.5 rounded-xl bg-primary text-[14px] font-bold text-screen"
        >
          <MaterialIcon name="upload_file" size={18} />
          {status ? '서류 올리고 다시 신청하기' : '서류 올리고 신청하기'}
        </button>
      )}
      <p className="mt-2.5 text-[11px] leading-normal font-medium text-primary-tint-ink opacity-80">
        서류는 담당자 1명만 확인하고, 검토가 끝나면 30일 안에 지웁니다.
      </p>
    </div>
  )
}

/** 인증 상태·우선배정 (6b, /mypage/verification). */
export default function MyVerificationPage() {
  const navigate = useNavigate()
  const { verifications, loading, error } = useMyVerifications()
  const byType = (type: MyVerification['verificationType']) => verifications.find((v) => v.verificationType === type)
  const freshman = byType('FRESHMAN')
  const { info, loading: infoLoading } = useMyInfo()

  return (
    <div className="flex flex-col pb-6">
      <TopBar title="인증 관리" onBack={() => navigate('/mypage')} />

      <div className="flex items-center gap-3.5 px-5 pt-1 pb-4">
        <span className="flex size-14 flex-none items-center justify-center rounded-full bg-primary-tint text-[22px] font-bold text-accent">
          {info ? nicknameInitial(info.nickname) : <MaterialIcon name="person" size={26} />}
        </span>
        <div className="min-w-0">
          <div className="truncate text-[17px] font-extrabold text-label">
            {info ? info.nickname : infoLoading ? '' : '정보를 불러오지 못했어요'}
          </div>
          <div className="mt-0.5 text-[13px] font-medium text-label-alt">
            {/* 로그인은 카카오만 지원한다 */}
            {info ? `카카오 로그인 · ${formatJoinedPeriod(info.createdAt)}` : infoLoading ? '불러오는 중…' : '잠시 후 다시 확인해 주세요'}
          </div>
        </div>
      </div>

      <div className="px-5">
        <h2 className="pb-2.5 font-hand text-[22px] font-bold text-label">인증 상태</h2>
        <div className="flex flex-col gap-2.5">
          {loading ? (
            <p className="py-4 text-center text-[13px] font-medium text-label-alt">인증 상태를 불러오는 중이에요…</p>
          ) : (
            <NeighborhoodRow verification={byType('NEIGHBORHOOD')} />
          )}
          {error && (
            <p role="alert" className="text-[13px] font-semibold text-terracotta">
              {error}
            </p>
          )}
        </div>

        <h2 className="pt-6 pb-1.5 font-hand text-[22px] font-bold text-label">우선배정 신청</h2>
        <p className="pb-2.5 text-[13px] leading-[1.6] font-medium text-ink-3">
          꼭 필요한 이웃에게 먼저 돌아가도록, 아래에 해당하면 서류를 추가로 올릴 수 있어요. 안 해도 신청은 할 수 있습니다.
        </p>
        {loading ? (
          <p className="py-4 text-center text-[13px] font-medium text-label-alt">인증 상태를 불러오는 중이에요…</p>
        ) : (
          <>
            {freshman ? <FreshmanCard verification={freshman} /> : <FreshmanRow />}
            <LowIncomeCard verification={byType('LOW_INCOME')} />
          </>
        )}

        <div className="mt-4 flex gap-2.5 rounded-2xl border border-border bg-surface px-4 py-3.5">
          <MaterialIcon name="help" size={18} className="mt-px flex-none text-label-alt" />
          <span className="text-[13px] leading-normal font-medium text-body">
            인증이 거절되면 사유와 함께 알려드리고, 서류를 고쳐서 다시 올릴 수 있어요.
          </span>
        </div>
      </div>
    </div>
  )
}
