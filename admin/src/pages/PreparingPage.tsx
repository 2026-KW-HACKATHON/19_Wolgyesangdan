import Card from '../components/Card'
import MaterialIcon from '../components/MaterialIcon'

/** 아직 만들지 않은 페이지 자리 — 각 화면 이슈에서 교체한다 */
export default function PreparingPage({ issue }: { issue: string }) {
  return (
    <Card className="flex flex-col items-center gap-2 px-6 py-16 text-center">
      <MaterialIcon name="construction" size={32} className="text-accent" />
      <p className="text-[15px] font-bold text-label">준비 중인 화면이에요</p>
      <p className="text-[13px] text-label-alt">{issue}에서 만들어요</p>
    </Card>
  )
}
