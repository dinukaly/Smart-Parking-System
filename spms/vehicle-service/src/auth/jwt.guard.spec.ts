import { ExecutionContext, UnauthorizedException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { Test, TestingModule } from '@nestjs/testing';
import * as jwt from 'jsonwebtoken';
import { JwtGuard } from './jwt.guard';

jest.mock('jsonwebtoken');

describe('JwtGuard', () => {
  let guard: JwtGuard;

  const mockPublicKey =
    '-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...\n-----END PUBLIC KEY-----';

  beforeEach(async () => {
    const mockConfigService = {
      get: jest
        .fn()
        .mockImplementation((key: string, defaultValue?: string) => {
          if (key === 'JWT_PUBLIC_KEY') {
            return mockPublicKey;
          }
          return defaultValue;
        }),
    };

    const module: TestingModule = await Test.createTestingModule({
      providers: [
        JwtGuard,
        {
          provide: ConfigService,
          useValue: mockConfigService,
        },
      ],
    }).compile();

    guard = module.get<JwtGuard>(JwtGuard);
    jest.clearAllMocks();
  });

  it('should be defined', () => {
    expect(guard).toBeDefined();
  });

  function createMockExecutionContext(authHeader?: string): {
    context: ExecutionContext;
    request: any;
  } {
    const request: any = {
      headers: authHeader ? { authorization: authHeader } : {},
    };

    const context = {
      switchToHttp: () => ({
        getRequest: () => request,
      }),
    } as unknown as ExecutionContext;

    return { context, request };
  }

  it('should throw UnauthorizedException when Authorization header is missing', () => {
    const { context } = createMockExecutionContext();

    expect(() => guard.canActivate(context)).toThrow(UnauthorizedException);
    expect(() => guard.canActivate(context)).toThrow(
      'Missing or invalid Authorization header',
    );
  });

  it('should throw UnauthorizedException when Authorization header does not start with Bearer', () => {
    const { context } = createMockExecutionContext('Basic dXNlcjpwYXNz');

    expect(() => guard.canActivate(context)).toThrow(UnauthorizedException);
  });

  it('should successfully validate a valid RS256 token and attach user to request', () => {
    const token = 'valid.jwt.token';
    const payload = {
      sub: 'user-uuid-123',
      email: 'driver@example.com',
      role: 'DRIVER',
      iss: 'spms-user-service',
      iat: 1234567890,
      exp: 1234571490,
    };

    (jwt.verify as jest.Mock).mockReturnValue(payload);

    const { context, request } = createMockExecutionContext(`Bearer ${token}`);

    const result = guard.canActivate(context);

    expect(result).toBe(true);
    expect(jwt.verify).toHaveBeenCalledWith(token, mockPublicKey, {
      algorithms: ['RS256'],
    });
    expect(request.user).toEqual(payload);
  });

  it('should throw UnauthorizedException when jwt verification fails', () => {
    const token = 'invalid.jwt.token';
    (jwt.verify as jest.Mock).mockImplementation(() => {
      throw new Error('jwt expired');
    });

    const { context } = createMockExecutionContext(`Bearer ${token}`);

    expect(() => guard.canActivate(context)).toThrow(UnauthorizedException);
    expect(() => guard.canActivate(context)).toThrow(
      'Invalid or expired token',
    );
  });
});
