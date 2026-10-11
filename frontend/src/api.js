const API_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081';

class ApiError extends Error {
  constructor(message, status) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

export async function apiRequest(path, { method = 'GET', token, body } = {}) {
  const response = await fetch(`${API_URL}${path}`, {
    method,
    headers: {
      ...(body ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    ...(body ? { body: JSON.stringify(body) } : {}),
  });
  if (response.status === 401 && token) {
    window.dispatchEvent(new Event('athletetrack:session-expired'));
  }
  let payload = null;
  if (response.status !== 204) {
    const contentType = response.headers.get('content-type') || '';
    if (contentType.includes('application/json')) {
      try {
        payload = await response.json();
      } catch (problem) {
        if (!(problem instanceof SyntaxError)) throw problem;
        throw new Error('The AthleteTrack server returned invalid JSON.');
      }
    } else if (!response.ok) {
      throw new ApiError(`Request failed (${response.status}).`, response.status);
    }
  }
  if (!response.ok) {
    throw new ApiError(payload?.detail || payload?.message || `Request failed (${response.status}).`, response.status);
  }
  return payload;
}
