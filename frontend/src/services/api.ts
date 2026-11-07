export const API_BASE_URL = '/api';

export class ApiError extends Error {
  public readonly status: number;

  constructor(
    message: string,
    status: number,
  ) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

export function buildApiUrl(path: string): string {
  return `${API_BASE_URL}${path}`;
}
