let csrfToken = ''

export class ApiError extends Error {
  constructor(message, details = {}) {
    super(message)
    this.name = 'ApiError'
    this.status = details.status
    this.code = details.code
    this.requestId = details.requestId
  }
}

export async function api(path, options = {}, allowCsrfRetry = true) {
  const method = String(options.method || 'GET').toUpperCase()
  const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) }
  if (!['GET', 'HEAD', 'OPTIONS'].includes(method) && csrfToken) {
    headers['X-CSRF-TOKEN'] = csrfToken
  }
  const response = await fetch(`/api${path}`, {
    ...options,
    headers,
  })
  const data = await response.json().catch(() => ({}))
  if (data.csrf_token) csrfToken = data.csrf_token
  if (!response.ok) {
    if (response.status === 403 && data.code === 'CSRF_INVALID' && allowCsrfRetry) {
      await api('/session', {}, false)
      return api(path, options, false)
    }
    if (response.status === 401 && path !== '/login' && path !== '/session') {
      window.dispatchEvent(new CustomEvent('labguard:unauthorized'))
    }
    throw new ApiError(data.message || '请求失败，请稍后重试', {
      status: response.status,
      code: data.code,
      requestId: data.requestId || data.request_id || response.headers.get('X-Request-ID'),
    })
  }
  return data
}

export function clearSecurityContext() {
  csrfToken = ''
}

export function jsonOptions(method, body) {
  return { method, body: JSON.stringify(body) }
}
