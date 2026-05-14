export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  email: string;
  fullName: string;
  role: string;
}

export interface CurrentUser {
  id: string;
  email: string;
  fullName: string;
  role: string;
  enabled: boolean;
  createdAt: string;
}
