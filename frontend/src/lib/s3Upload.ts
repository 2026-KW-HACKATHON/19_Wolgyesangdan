/**
 * 서버가 발급한 presigned URL로 S3에 파일을 바로 올린다 (물품 사진·우선배정 서류 공통).
 * Content-Type이 서명에 포함되므로 발급 요청과 같은 값으로 PUT 해야 한다 (다르면 S3가 403).
 * S3로 바로 보내는 요청이라 apiFetch(우리 서버 주소·Authorization 헤더)를 쓰지 않는다.
 */
export async function putToS3(uploadUrl: string, file: File, contentType: string) {
  const response = await fetch(uploadUrl, { method: 'PUT', headers: { 'Content-Type': contentType }, body: file })
  if (!response.ok) throw new Error(`S3 업로드 실패 (${response.status})`)
}
