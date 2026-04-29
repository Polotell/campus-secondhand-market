import request from '@/utils/request'

/**
 * 上传图片到后端 /api/file/upload
 * @param {File} file
 * @returns {Promise<{url:string}>}
 */
export function uploadImage(file) {
  const fd = new FormData()
  fd.append('file', file)
  return request.post('/file/upload', fd, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
