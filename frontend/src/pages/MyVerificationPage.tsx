import { useNavigate } from 'react-router-dom'
import MaterialIcon from '../components/icons/MaterialIcon'
import TopBar from '../components/TopBar'
import { PROFILE, VERIFICATION, type VerificationStatus } from '../data/mypage'

const STUDENT_STEPS = ['서류 접수', '검토 중', '완료'] as const

/** 상태별로 3단계 진행 바에서 어디까지 채울지 정한다. */
function stepIndex(status: VerificationStatus) {
  if (status === 'approved') return 3
  if (status === 'reviewing' || status === 'rejected') return 2
  if (status === 'submitted') return 1
  return 0
}

function StudentCard() {
  const { status, submittedAt, school, fileCount } = VERIFICATION.student
  const reviewing = status === 'reviewing' || status === 'submitted'
  const done = stepIndex(status)

  return (
    <div className="rounded-[18px] border border-border bg-surface px-4 py-4">
      <div className="flex items-center gap-2.5">
        <span className="flex size-[38px] flex-none items-center justify-center rounded-xl bg-amber-tint text-amber-ink">
          <MaterialIcon name="hourglass_top" size={20} />
        </span>
        <div className="min-w-0 flex-1">
          <div className="text-[15px] font-bold text-label">학생 인증</div>
          <div className="mt-0.5 text-[12px] font-medium text-label-alt">
            {submittedAt} 신청 · {school} · 서류 {fileCount}장
          </div>
        </div>
        <span
          className={`flex-none rounded-[7px] px-2 py-1 text-[11px] font-bold ${
            reviewing ? 'bg-amber-badge text-amber-badge-ink' : 'bg-primary-tint text-primary-tint-ink'
          }`}
        >
          {reviewing ? '검토 중' : status === 'approved' ? '인증 완료' : '다시 제출 필요'}
        </span>
      </div>

      <div className="mt-3.5 flex gap-1.5">
        {STUDENT_STEPS.map((_, i) => (
          <span
            key={i}
            className={`h-[5px] flex-1 rounded-[3px] ${
              i < done - 1 || (i === done - 1 && status === 'approved')
                ? 'bg-primary'
                : i === done - 1
                  ? 'bg-amber-step'
                  : 'bg-track'
            }`}
          />
        ))}
      </div>
      <div className="mt-1.5 flex">
        {STUDENT_STEPS.map((label, i) => (
          <span
            key={label}
            className={`flex-1 text-[11px] font-bold ${
              i === 0 ? 'text-left' : i === STUDENT_STEPS.length - 1 ? 'text-right' : 'text-center'
            } ${i < done ? 'text-accent' : 'font-medium text-label-alt'}`}
          >
            {label}
          </span>
        ))}
      </div>

      <p className="mt-2.5 text-[12px] leading-normal font-medium text-label-alt">
        보통 1~2일(평일 기준) 안에 끝나요. 결과는 알림으로 알려드려요.
      </p>

      <div className="mt-3 flex gap-2">
        {/* TODO: 제출 서류 조회 / 재업로드 화면 연결 */}
        <button type="button" className="flex-1 cursor-pointer rounded-xl border border-border bg-surface py-2.5 text-[13px] font-bold text-ink-2">
          제출 서류 보기
        </button>
        <button type="button" className="flex-1 cursor-pointer rounded-xl border border-border bg-surface py-2.5 text-[13px] font-bold text-ink-2">
          서류 다시 올리기
        </button>
      </div>
    </div>
  )
}

function ResidentRow() {
  const navigate = useNavigate()
  return (
    <div className="flex items-center gap-2.5 rounded-[18px] border border-border bg-surface px-4 py-4">
      <span className="flex size-[38px] flex-none items-center justify-center rounded-xl bg-sunken text-label-alt">
        <MaterialIcon name="home_work" size={20} />
      </span>
      <div className="min-w-0 flex-1">
        <div className="text-[15px] font-bold text-label">월계 주민 인증</div>
        <div className="mt-0.5 text-[12px] font-medium text-label-alt">둘 중 하나만 인증하면 참여할 수 있어요</div>
      </div>
      <button
        type="button"
        onClick={() => navigate('/verification/resident')}
        className="flex-none cursor-pointer text-[13px] font-bold text-accent"
      >
        추가
      </button>
    </div>
  )
}

function LowIncomeCard() {
  return (
    <div className="rounded-[18px] bg-primary-tint px-4 py-4">
      <div className="flex items-center gap-2.5">
        <span className="flex size-[38px] flex-none items-center justify-center rounded-xl bg-surface text-accent">
          <MaterialIcon name="volunteer_activism" size={20} />
        </span>
        <div className="min-w-0 flex-1">
          <div className="text-[15px] font-bold text-primary-tint-ink">저소득층 인증</div>
          <div className="mt-0.5 text-[12px] font-medium text-primary-tint-ink opacity-80">대기열 가산점이 적용돼요</div>
        </div>
        <span className="flex-none rounded-[7px] bg-surface px-2 py-1 text-[11px] font-bold text-body">선택</span>
      </div>
      <div className="mt-3 flex flex-wrap gap-1.5">
        {['수급자 증명서', '차상위 확인서', '한부모가족 증명서'].map((doc) => (
          <span key={doc} className="rounded-[7px] bg-surface px-2 py-1 text-[11px] font-semibold text-primary-tint-ink">
            {doc}
          </span>
        ))}
      </div>
      {/* TODO: 저소득층 서류 첨부 화면(type=lowIncome) 연결. 지금은 서류 종류만 다른 주민 폼과 같은 구조라 아직 연결하지 않았다. */}
      <button
        type="button"
        disabled
        className="mt-3.5 flex h-[46px] w-full cursor-default items-center justify-center gap-1.5 rounded-xl bg-primary text-[14px] font-bold text-screen opacity-60"
      >
        <MaterialIcon name="upload_file" size={18} />
        서류 올리고 신청하기
      </button>
      <p className="mt-2.5 text-[11px] leading-normal font-medium text-primary-tint-ink opacity-80">
        서류는 담당자 1명만 확인하고, 검토가 끝나면 30일 안에 지웁니다.
      </p>
    </div>
  )
}

/** 인증 상태·우선배정 (6b, /mypage/verification). */
export default function MyVerificationPage() {
  const navigate = useNavigate()
  const hasStudent = VERIFICATION.student.status !== 'none'

  return (
    <div className="flex flex-col pb-6">
      <TopBar title="인증 관리" onBack={() => navigate('/mypage')} />

      <div className="flex items-center gap-3.5 px-5 pt-1 pb-4">
        <span className="flex size-14 flex-none items-center justify-center rounded-full bg-primary-tint text-[22px] font-bold text-accent">
          {PROFILE.initial}
        </span>
        <div>
          <div className="text-[17px] font-extrabold text-label">{PROFILE.name}</div>
          <div className="mt-0.5 text-[13px] font-medium text-label-alt">
            {PROFILE.provider} 로그인 · 가입 {PROFILE.joinedMonths}개월
          </div>
        </div>
      </div>

      <div className="px-5">
        <h2 className="pb-2.5 font-hand text-[22px] font-bold text-label">인증 상태</h2>
        <div className="flex flex-col gap-2.5">
          {hasStudent && <StudentCard />}
          <ResidentRow />
        </div>

        <h2 className="pt-6 pb-1.5 font-hand text-[22px] font-bold text-label">우선배정 신청</h2>
        <p className="pb-2.5 text-[13px] leading-[1.6] font-medium text-ink-3">
          꼭 필요한 이웃에게 먼저 돌아가도록, 아래에 해당하면 서류를 추가로 올릴 수 있어요. 안 해도 신청은 할 수 있습니다.
        </p>
        <LowIncomeCard />

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
