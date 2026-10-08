import { useSyncExternalStore } from 'react'
import type { UploadedFile } from '../types/verification'

// 물품 등록 중인 사진을 화면 밖(모듈)에 보관한다 (#161).
// 등록 화면의 state에만 두면 연락 수단 설정 화면에 다녀오는 동안 화면이 사라지면서 사진도 사라진다.
// - 화면을 오가는 동안: 올리는 중인 사진까지 그대로 남고, 올리기가 끝나면 여기에 반영된다
// - 새로고침 뒤: 다 올라간 사진만 localStorage에서 복원한다 (서버 주소 imageUrl로 다시 보여준다)
//   로컬 미리보기 주소(url)는 새로고침하면 못 쓰게 되고, 올리던 중인 사진은 끊기므로 저장하지 않는다

const STORAGE_KEY = 'draft:register:photos'

function loadSaved(): UploadedFile[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    const saved: unknown = raw ? JSON.parse(raw) : []
    if (!Array.isArray(saved)) return []
    return saved.flatMap((entry: Partial<UploadedFile>) =>
      typeof entry.id === 'string' && typeof entry.name === 'string' && typeof entry.imageUrl === 'string'
        ? [{ id: entry.id, name: entry.name, size: Number(entry.size) || 0, status: 'done' as const, imageUrl: entry.imageUrl }]
        : [],
    )
  } catch {
    return []
  }
}

function persist(photos: UploadedFile[]) {
  try {
    const uploaded = photos.flatMap(({ id, name, size, status, imageUrl }) =>
      status === 'done' && imageUrl ? [{ id, name, size, imageUrl }] : [],
    )
    if (uploaded.length > 0) localStorage.setItem(STORAGE_KEY, JSON.stringify(uploaded))
    else localStorage.removeItem(STORAGE_KEY)
  } catch {
    // 저장소를 쓸 수 없는 환경(사생활 보호 모드 등)에서는 조용히 넘어간다 — 화면을 오가는 동안은 그대로 남는다
  }
}

/** 로컬 미리보기 주소(URL.createObjectURL)를 해제한다 — 해제하지 않으면 사진이 메모리에 계속 남는다 */
function revokePreviews(photos: UploadedFile[]) {
  for (const photo of photos) {
    if (photo.url) URL.revokeObjectURL(photo.url)
  }
}

let photos: UploadedFile[] = loadSaved()
const listeners = new Set<() => void>()

function commit(next: UploadedFile[]) {
  photos = next
  persist(next)
  for (const listener of listeners) listener()
}

function subscribe(listener: () => void) {
  listeners.add(listener)
  return () => {
    listeners.delete(listener)
  }
}

export const registerPhotoStore = {
  /** 지금 보관 중인 사진 (화면 밖에서 올리기가 끝난 시점의 개수 확인 등에 쓴다) */
  get: () => photos,
  add(entries: UploadedFile[]) {
    commit([...photos, ...entries])
  },
  /** 올리기가 끝났을 때 상태·서버 주소를 반영한다. 그사이 지운 사진이면 아무 일도 없다 */
  update(id: string, patch: Partial<UploadedFile>) {
    commit(photos.map((photo) => (photo.id === id ? { ...photo, ...patch } : photo)))
  },
  remove(id: string) {
    revokePreviews(photos.filter((photo) => photo.id === id))
    commit(photos.filter((photo) => photo.id !== id))
  },
  /** 등록을 마쳤을 때 전부 비운다 */
  clear() {
    revokePreviews(photos)
    commit([])
  },
}

/** 등록 중인 사진 목록. 보관소가 바뀌면 화면이 다시 그려진다 */
export function useRegisterPhotos() {
  return useSyncExternalStore(subscribe, registerPhotoStore.get)
}
