import { INestApplication, ValidationPipe } from '@nestjs/common';
import { Test, TestingModule } from '@nestjs/testing';
import { getRepositoryToken } from '@nestjs/typeorm';
import request = require('supertest');
import { JwtGuard } from '../src/auth/jwt.guard';
import {
  EntryExitLog,
  VehicleEventType,
} from '../src/vehicles/entities/entry-exit-log.entity';
import { Vehicle, VehicleType } from '../src/vehicles/entities/vehicle.entity';
import { VehiclesModule } from '../src/vehicles/vehicles.module';

describe('VehiclesController (e2e)', () => {
  let app: INestApplication;

  const mockUserId = '11111111-1111-1111-1111-111111111111';
  const mockVehicleId = '33333333-3333-3333-3333-333333333333';

  const mockVehicle: Vehicle = {
    id: mockVehicleId,
    userId: mockUserId,
    licensePlate: 'ABC-1234',
    make: 'Toyota',
    model: 'Corolla',
    color: 'Silver',
    vehicleType: VehicleType.CAR,
    createdAt: new Date(),
    updatedAt: new Date(),
    entryExitLogs: [],
  };

  const mockVehicleRepository = {
    findOne: jest.fn(),
    find: jest.fn(),
    create: jest.fn(),
    save: jest.fn(),
    remove: jest.fn(),
  };

  const mockEntryExitLogRepository = {
    create: jest.fn(),
    save: jest.fn(),
  };

  beforeAll(async () => {
    const moduleFixture: TestingModule = await Test.createTestingModule({
      imports: [VehiclesModule],
    })
      .overrideProvider(getRepositoryToken(Vehicle))
      .useValue(mockVehicleRepository)
      .overrideProvider(getRepositoryToken(EntryExitLog))
      .useValue(mockEntryExitLogRepository)
      .overrideGuard(JwtGuard)
      .useValue({
        canActivate: (context: any) => {
          const req = context.switchToHttp().getRequest();
          req.user = {
            sub: mockUserId,
            email: 'driver@example.com',
            role: 'DRIVER',
          };
          return true;
        },
      })
      .compile();

    app = moduleFixture.createNestApplication();
    app.useGlobalPipes(
      new ValidationPipe({
        whitelist: true,
        transform: true,
        forbidNonWhitelisted: true,
      }),
    );
    await app.init();
  });

  afterAll(async () => {
    await app.close();
  });

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('POST /api/vehicles - should register a new vehicle (201)', async () => {
    mockVehicleRepository.findOne.mockResolvedValue(null);
    mockVehicleRepository.create.mockReturnValue(mockVehicle);
    mockVehicleRepository.save.mockResolvedValue(mockVehicle);

    const response = await request(app.getHttpServer())
      .post('/api/vehicles')
      .send({
        licensePlate: 'ABC-1234',
        make: 'Toyota',
        model: 'Corolla',
        color: 'Silver',
        vehicleType: 'CAR',
      })
      .expect(201);

    expect(response.body.id).toEqual(mockVehicleId);
    expect(response.body.licensePlate).toEqual('ABC-1234');
  });

  it('GET /api/vehicles - should list current user vehicles (200)', async () => {
    mockVehicleRepository.find.mockResolvedValue([mockVehicle]);

    const response = await request(app.getHttpServer())
      .get('/api/vehicles')
      .expect(200);

    expect(Array.isArray(response.body)).toBe(true);
    expect(response.body.length).toBe(1);
    expect(response.body[0].id).toEqual(mockVehicleId);
  });

  it('GET /api/vehicles/:id - should get vehicle by ID (200)', async () => {
    mockVehicleRepository.findOne.mockResolvedValue(mockVehicle);

    const response = await request(app.getHttpServer())
      .get(`/api/vehicles/${mockVehicleId}`)
      .expect(200);

    expect(response.body.id).toEqual(mockVehicleId);
  });

  it('PUT /api/vehicles/:id - should update vehicle (200)', async () => {
    mockVehicleRepository.findOne.mockResolvedValue(mockVehicle);
    const updated = { ...mockVehicle, color: 'Black' };
    mockVehicleRepository.save.mockResolvedValue(updated);

    const response = await request(app.getHttpServer())
      .put(`/api/vehicles/${mockVehicleId}`)
      .send({ color: 'Black' })
      .expect(200);

    expect(response.body.color).toEqual('Black');
  });

  it('DELETE /api/vehicles/:id - should remove vehicle (204)', async () => {
    mockVehicleRepository.findOne.mockResolvedValue(mockVehicle);
    mockVehicleRepository.remove.mockResolvedValue(mockVehicle);

    await request(app.getHttpServer())
      .delete(`/api/vehicles/${mockVehicleId}`)
      .expect(204);
  });

  it('POST /api/vehicles/:id/entry - should log vehicle entry (200)', async () => {
    mockVehicleRepository.findOne.mockResolvedValue(mockVehicle);
    const mockLog: EntryExitLog = {
      id: 'log-entry-1',
      vehicleId: mockVehicleId,
      parkingSpaceId: 'space-123',
      eventType: VehicleEventType.ENTRY,
      timestamp: new Date(),
      vehicle: mockVehicle,
    };
    mockEntryExitLogRepository.create.mockReturnValue(mockLog);
    mockEntryExitLogRepository.save.mockResolvedValue(mockLog);

    const response = await request(app.getHttpServer())
      .post(`/api/vehicles/${mockVehicleId}/entry`)
      .send({ parkingSpaceId: 'space-123' })
      .expect(200);

    expect(response.body.eventType).toEqual('ENTRY');
  });

  it('POST /api/vehicles/:id/exit - should log vehicle exit (200)', async () => {
    mockVehicleRepository.findOne.mockResolvedValue(mockVehicle);
    const mockLog: EntryExitLog = {
      id: 'log-exit-1',
      vehicleId: mockVehicleId,
      parkingSpaceId: 'space-123',
      eventType: VehicleEventType.EXIT,
      timestamp: new Date(),
      vehicle: mockVehicle,
    };
    mockEntryExitLogRepository.create.mockReturnValue(mockLog);
    mockEntryExitLogRepository.save.mockResolvedValue(mockLog);

    const response = await request(app.getHttpServer())
      .post(`/api/vehicles/${mockVehicleId}/exit`)
      .send({ parkingSpaceId: 'space-123' })
      .expect(200);

    expect(response.body.eventType).toEqual('EXIT');
  });
});
