/** 아바타에 넣을 닉네임 첫 글자 (이모지 등 서로게이트 쌍도 한 글자로) */
export function nicknameInitial(nickname: string) {
  return Array.from(nickname.trim())[0] ?? ''
}

/** 가입한 지 꽉 찬 개월 수로 "가입 N개월". 한 달이 안 됐으면 "가입 1개월 미만" */
export function formatJoinedPeriod(createdAt: string, now = new Date()) {
  const joined = new Date(createdAt)
  let months = (now.getFullYear() - joined.getFullYear()) * 12 + (now.getMonth() - joined.getMonth())
  if (now.getDate() < joined.getDate()) months -= 1
  return months < 1 ? '가입 1개월 미만' : `가입 ${months}개월`
}
