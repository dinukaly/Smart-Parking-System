import {
  ConflictException,
  ForbiddenException,
  NotFoundException,
} from '@nestjs/common';
import { Test, TestingModule } from '@nestjs/testing';
import { getRepositoryToken } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { CreateVehicleDto } from './dto/create-vehicle.dto';
import { EntryExitDto } from './dto/entry-exit.dto';
import { UpdateVehicleDto } from './dto/update-vehicle.dto';
import {
  EntryExitLog,
  VehicleEventType,
} from './entities/entry-exit-log.entity';
import { Vehicle, VehicleType } from './entities/vehicle.entity';
import { VehiclesService } from './vehicles.service';

describe('VehiclesService', () => {
  let service: VehiclesService;
  let vehicleRepo: jest.Mocked<Repository<Vehicle>>;
  let entryExitLogRepo: jest.Mocked<Repository<EntryExitLog>>;

  const mockUserId = '11111111-1111-1111-1111-111111111111';
  const mockOtherUserId = '22222222-2222-2222-2222-222222222222';
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

  beforeEach(async () => {
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

    const module: TestingModule = await Test.createTestingModule({
      providers: [
        VehiclesService,
        {
          provide: getRepositoryToken(Vehicle),
          useValue: mockVehicleRepository,
        },
        {
          provide: getRepositoryToken(EntryExitLog),
          useValue: mockEntryExitLogRepository,
        },
      ],
    }).compile();

    service = module.get<VehiclesService>(VehiclesService);
    vehicleRepo = module.get(getRepositoryToken(Vehicle));
    entryExitLogRepo = module.get(getRepositoryToken(EntryExitLog));
  });

  it('should be defined', () => {
    expect(service).toBeDefined();
  });

  describe('create', () => {
    const dto: CreateVehicleDto = {
      licensePlate: 'ABC-1234',
      make: 'Toyota',
      model: 'Corolla',
      color: 'Silver',
      vehicleType: VehicleType.CAR,
    };

    it('should successfully create a new vehicle', async () => {
      vehicleRepo.findOne.mockResolvedValue(null);
      vehicleRepo.create.mockReturnValue(mockVehicle);
      vehicleRepo.save.mockResolvedValue(mockVehicle);

      const result = await service.create(mockUserId, dto);

      expect(vehicleRepo.findOne).toHaveBeenCalledWith({
        where: { licensePlate: dto.licensePlate },
      });
      expect(vehicleRepo.create).toHaveBeenCalledWith({
        userId: mockUserId,
        licensePlate: dto.licensePlate,
        make: dto.make,
        model: dto.model,
        color: dto.color,
        vehicleType: dto.vehicleType,
      });
      expect(vehicleRepo.save).toHaveBeenCalledWith(mockVehicle);
      expect(result).toEqual(mockVehicle);
    });

    it('should throw ConflictException if license plate already exists', async () => {
      vehicleRepo.findOne.mockResolvedValue(mockVehicle);

      await expect(service.create(mockUserId, dto)).rejects.toThrow(
        ConflictException,
      );
      expect(vehicleRepo.save).not.toHaveBeenCalled();
    });
  });

  describe('findAllByUser', () => {
    it('should return an array of vehicles belonging to user', async () => {
      const vehicles = [mockVehicle];
      vehicleRepo.find.mockResolvedValue(vehicles);

      const result = await service.findAllByUser(mockUserId);

      expect(vehicleRepo.find).toHaveBeenCalledWith({
        where: { userId: mockUserId },
        order: { createdAt: 'DESC' },
      });
      expect(result).toEqual(vehicles);
    });
  });

  describe('findOne', () => {
    it('should return vehicle if found and belongs to user', async () => {
      vehicleRepo.findOne.mockResolvedValue(mockVehicle);

      const result = await service.findOne(mockVehicleId, mockUserId);

      expect(vehicleRepo.findOne).toHaveBeenCalledWith({
        where: { id: mockVehicleId },
      });
      expect(result).toEqual(mockVehicle);
    });

    it('should throw NotFoundException if vehicle does not exist', async () => {
      vehicleRepo.findOne.mockResolvedValue(null);

      await expect(service.findOne(mockVehicleId, mockUserId)).rejects.toThrow(
        NotFoundException,
      );
    });

    it('should throw ForbiddenException if vehicle belongs to another user', async () => {
      vehicleRepo.findOne.mockResolvedValue(mockVehicle);

      await expect(
        service.findOne(mockVehicleId, mockOtherUserId),
      ).rejects.toThrow(ForbiddenException);
    });
  });

  describe('update', () => {
    const updateDto: UpdateVehicleDto = {
      color: 'Black',
      model: 'Camry',
    };

    it('should update vehicle successfully', async () => {
      vehicleRepo.findOne.mockResolvedValue(mockVehicle);
      const updatedVehicle = { ...mockVehicle, ...updateDto };
      vehicleRepo.save.mockResolvedValue(updatedVehicle);

      const result = await service.update(mockVehicleId, mockUserId, updateDto);

      expect(vehicleRepo.save).toHaveBeenCalled();
      expect(result.color).toEqual('Black');
    });

    it('should throw ConflictException if updated license plate belongs to another vehicle', async () => {
      vehicleRepo.findOne
        .mockResolvedValueOnce(mockVehicle) // findOne by id
        .mockResolvedValueOnce({
          ...mockVehicle,
          id: 'different-id',
        }); // findOne for duplicate plate

      await expect(
        service.update(mockVehicleId, mockUserId, { licensePlate: 'XYZ-9999' }),
      ).rejects.toThrow(ConflictException);
    });
  });

  describe('remove', () => {
    it('should remove vehicle if found and owned by user', async () => {
      vehicleRepo.findOne.mockResolvedValue(mockVehicle);
      vehicleRepo.remove.mockResolvedValue(mockVehicle);

      await service.remove(mockVehicleId, mockUserId);

      expect(vehicleRepo.remove).toHaveBeenCalledWith(mockVehicle);
    });
  });

  describe('logEntry', () => {
    const entryDto: EntryExitDto = {
      parkingSpaceId: 'space-123',
    };

    it('should log an ENTRY event for vehicle', async () => {
      vehicleRepo.findOne.mockResolvedValue(mockVehicle);

      const mockLog: EntryExitLog = {
        id: 'log-1',
        vehicleId: mockVehicleId,
        parkingSpaceId: 'space-123',
        eventType: VehicleEventType.ENTRY,
        timestamp: new Date(),
        vehicle: mockVehicle,
      };

      entryExitLogRepo.create.mockReturnValue(mockLog);
      entryExitLogRepo.save.mockResolvedValue(mockLog);

      const result = await service.logEntry(
        mockVehicleId,
        mockUserId,
        entryDto,
      );

      expect(entryExitLogRepo.create).toHaveBeenCalledWith({
        vehicleId: mockVehicleId,
        parkingSpaceId: entryDto.parkingSpaceId,
        eventType: VehicleEventType.ENTRY,
      });
      expect(entryExitLogRepo.save).toHaveBeenCalledWith(mockLog);
      expect(result.eventType).toEqual(VehicleEventType.ENTRY);
    });
  });

  describe('logExit', () => {
    const exitDto: EntryExitDto = {
      parkingSpaceId: 'space-123',
    };

    it('should log an EXIT event for vehicle', async () => {
      vehicleRepo.findOne.mockResolvedValue(mockVehicle);

      const mockLog: EntryExitLog = {
        id: 'log-2',
        vehicleId: mockVehicleId,
        parkingSpaceId: 'space-123',
        eventType: VehicleEventType.EXIT,
        timestamp: new Date(),
        vehicle: mockVehicle,
      };

      entryExitLogRepo.create.mockReturnValue(mockLog);
      entryExitLogRepo.save.mockResolvedValue(mockLog);

      const result = await service.logExit(mockVehicleId, mockUserId, exitDto);

      expect(entryExitLogRepo.create).toHaveBeenCalledWith({
        vehicleId: mockVehicleId,
        parkingSpaceId: exitDto.parkingSpaceId,
        eventType: VehicleEventType.EXIT,
      });
      expect(entryExitLogRepo.save).toHaveBeenCalledWith(mockLog);
      expect(result.eventType).toEqual(VehicleEventType.EXIT);
    });
  });
});
