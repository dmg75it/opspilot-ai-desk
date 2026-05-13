export interface User {
  id: number;
  email: string;
  fullName: string;
  role: 'ADMIN' | 'OPERATOR';
}

export interface AuthResponse {
  token: string;
  user: User;
}
