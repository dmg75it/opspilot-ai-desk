export interface User {
  id: string;
  email: string;
  role: 'ADMIN' | 'OPERATOR';
  active: boolean;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  email: string;
  role: string;
}
