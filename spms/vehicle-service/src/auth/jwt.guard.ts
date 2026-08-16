import {
  CanActivate,
  ExecutionContext,
  Injectable,
  Logger,
  UnauthorizedException,
} from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { Request } from 'express';
import * as jwt from 'jsonwebtoken';

export interface JwtPayload {
  sub: string; // userId
  email: string;
  role: string;
  iss: string;
  iat: number;
  exp: number;
}

/**
 * Zero-Trust JWT Guard for Vehicle Service.
 * Independently validates the RSA-signed (RS256) JWT access token
 * using the shared RSA Public Key — no call to User Service required.
 *
 * Attaches the decoded payload to request.user for downstream use.
 */
@Injectable()
export class JwtGuard implements CanActivate {
  private readonly logger = new Logger(JwtGuard.name);
  private readonly publicKey: string;

  constructor(private readonly configService: ConfigService) {
    const rawKey = this.configService.get<string>('JWT_PUBLIC_KEY', '');
    // Support both escaped (\n) and real newline formats
    this.publicKey = rawKey.replace(/\\n/g, '\n');
  }

  canActivate(context: ExecutionContext): boolean {
    const request = context.switchToHttp().getRequest<Request>();
    const token = this.extractBearerToken(request);

    if (!token) {
      throw new UnauthorizedException(
        'Missing or invalid Authorization header',
      );
    }

    try {
      const payload = jwt.verify(token, this.publicKey, {
        algorithms: ['RS256'],
      }) as JwtPayload;

      // Attach decoded user context to the request
      (request as any).user = payload;
      return true;
    } catch (error) {
      this.logger.warn(`JWT validation failed: ${(error as Error).message}`);
      throw new UnauthorizedException('Invalid or expired token');
    }
  }

  private extractBearerToken(request: Request): string | null {
    const authHeader = request.headers['authorization'];
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return null;
    }
    return authHeader.substring(7);
  }
}
