import http from './http';
import type { Result, User } from '../types';

export interface LoginRequest {
    email: string;
    password: string;
}

export interface RegisterRequest {
    username: string;
    email: string;
    password: string;
  }

export const login = (data: LoginRequest): Promise<string> =>
    http.post<Result<string>>('/api/auth/login', data).then(r => r.data.data);

export const register = (data: RegisterRequest): Promise<User> =>
    http.post<Result<User>>('/api/auth/register', data).then(r => r.data.data);
