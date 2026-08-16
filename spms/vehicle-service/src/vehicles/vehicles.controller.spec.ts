import { Test, TestingModule } from '@nestjs/testing';
import { Request } from 'express';
import { JwtGuard, JwtPayload } from '../auth/jwt.guard';
import { CreateVehicleDto } from './dto/create-vehicle.dto';
import { EntryExitDto } from './dto/entry-exit.dto';
import { UpdateVehicleDto } from './dto/update-vehicle.dto';
import {
  EntryExitLog,
  VehicleEventType,
} from './entities/entry-exit-log.entity';
import { Vehicle, VehicleType } from './entities/vehicle.entity';
import { VehiclesController } from './vehicles.controller';
import { VehiclesService } from './vehicles.service';

describe('VehiclesController', () => {
  let controller: VehiclesController;
  let service: jest.Mocked<VehiclesService>;

  const mockUserId = '11111111-1111-1111-1111-111111111111';
  const mockVehicleId = '33333333-3333-3333-3333-333333333333';

  const mockUserPayload: JwtPayload = {
    sub: mockUserId,
    email: 'driver@example.com',
    role: 'DRIVER',
    iss: 'spms-user-service',
    iat: Math.floor(Date.now() / 1000),
    exp: Math.floor(Date.now() / 1000) + 3600,
  };

  const mockRequest = {
    user: mockUserPayload,
  } as unknown as Request;

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

  beforeEach(async () => {
    const mockVehiclesService = {
      create: jest.fn(),
      findAllByUser: jest.fn(),
      findOne: jest.fn(),
      update: jest.fn(),
      remove: jest.fn(),
      logEntry: jest.fn(),
      logExit: jest.fn(),
    };

    const module: TestingModule = await Test.createTestingModule({
      controllers: [VehiclesController],
      providers: [
        {
          provide: VehiclesService,
          useValue: mockVehiclesService,
        },
      ],
    })
      .overrideGuard(JwtGuard)
      .useValue({ canActivate: () => true })
      .compile();

    controller = module.get<VehiclesController>(VehiclesController);
    service = module.get(VehiclesService);
  });

  it('should be defined', () => {
    expect(controller).toBeDefined();
  });

  describe('create', () => {
    it('should create and return a vehicle', async () => {
      const dto: CreateVehicleDto = {
        licensePlate: 'ABC-1234',
        make: 'Toyota',
        model: 'Corolla',
        color: 'Silver',
        vehicleType: VehicleType.CAR,
      };

      service.create.mockResolvedValue(mockVehicle);

      const result = await controller.create(mockRequest, dto);

      expect(service.create).toHaveBeenCalledWith(mockUserId, dto);
      expect(result).toEqual(mockVehicle);
    });
  });

  describe('findAll', () => {
    it('should return all vehicles for the authenticated user', async () => {
      service.findAllByUser.mockResolvedValue([mockVehicle]);

      const result = await controller.findAll(mockRequest);

      expect(service.findAllByUser).toHaveBeenCalledWith(mockUserId);
      expect(result).toEqual([mockVehicle]);
    });
  });

  describe('findOne', () => {
    it('should return a vehicle by id', async () => {
      service.findOne.mockResolvedValue(mockVehicle);

      const result = await controller.findOne(mockRequest, mockVehicleId);

      expect(service.findOne).toHaveBeenCalledWith(mockVehicleId, mockUserId);
      expect(result).toEqual(mockVehicle);
    });
  });

  describe('update', () => {
    it('should update and return the updated vehicle', async () => {
      const dto: UpdateVehicleDto = { color: 'Blue' };
      const updated = { ...mockVehicle, color: 'Blue' };
      service.update.mockResolvedValue(updated);

      const result = await controller.update(mockRequest, mockVehicleId, dto);

      expect(service.update).toHaveBeenCalledWith(
        mockVehicleId,
        mockUserId,
        dto,
      );
      expect(result).toEqual(updated);
    });
  });

  describe('remove', () => {
    it('should delete a vehicle', async () => {
      service.remove.mockResolvedValue(undefined);

      await controller.remove(mockRequest, mockVehicleId);

      expect(service.remove).toHaveBeenCalledWith(mockVehicleId, mockUserId);
    });
  });

  describe('logEntry', () => {
    it('should log vehicle entry and return entry exit log', async () => {
      const dto: EntryExitDto = { parkingSpaceId: 'space-999' };
      const mockLog: EntryExitLog = {
        id: 'log-1',
        vehicleId: mockVehicleId,
        parkingSpaceId: 'space-999',
        eventType: VehicleEventType.ENTRY,
        timestamp: new Date(),
        vehicle: mockVehicle,
      };

      service.logEntry.mockResolvedValue(mockLog);

      const result = await controller.logEntry(mockRequest, mockVehicleId, dto);

      expect(service.logEntry).toHaveBeenCalledWith(
        mockVehicleId,
        mockUserId,
        dto,
      );
      expect(result).toEqual(mockLog);
    });
  });

  describe('logExit', () => {
    it('should log vehicle exit and return entry exit log', async () => {
      const dto: EntryExitDto = { parkingSpaceId: 'space-999' };
      const mockLog: EntryExitLog = {
        id: 'log-2',
        vehicleId: mockVehicleId,
        parkingSpaceId: 'space-999',
        eventType: VehicleEventType.EXIT,
        timestamp: new Date(),
        vehicle: mockVehicle,
      };

      service.logExit.mockResolvedValue(mockLog);

      const result = await controller.logExit(mockRequest, mockVehicleId, dto);

      expect(service.logExit).toHaveBeenCalledWith(
        mockVehicleId,
        mockUserId,
        dto,
      );
      expect(result).toEqual(mockLog);
    });
  });
});
