import {
  ConflictException,
  ForbiddenException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { CreateVehicleDto } from './dto/create-vehicle.dto';
import { UpdateVehicleDto } from './dto/update-vehicle.dto';
import { EntryExitDto } from './dto/entry-exit.dto';
import {
  EntryExitLog,
  VehicleEventType,
} from './entities/entry-exit-log.entity';
import { Vehicle } from './entities/vehicle.entity';

@Injectable()
export class VehiclesService {
  constructor(
    @InjectRepository(Vehicle)
    private readonly vehicleRepository: Repository<Vehicle>,
    @InjectRepository(EntryExitLog)
    private readonly entryExitLogRepository: Repository<EntryExitLog>,
  ) {}

  async create(userId: string, dto: CreateVehicleDto): Promise<Vehicle> {
    const existing = await this.vehicleRepository.findOne({
      where: { licensePlate: dto.licensePlate },
    });
    if (existing) {
      throw new ConflictException(
        `Vehicle with license plate ${dto.licensePlate} is already registered`,
      );
    }

    const vehicle = this.vehicleRepository.create({
      userId,
      licensePlate: dto.licensePlate,
      make: dto.make,
      model: dto.model,
      color: dto.color,
      vehicleType: dto.vehicleType,
    });

    return this.vehicleRepository.save(vehicle);
  }

  async findAllByUser(userId: string): Promise<Vehicle[]> {
    return this.vehicleRepository.find({
      where: { userId },
      order: { createdAt: 'DESC' },
    });
  }

  async findOne(id: string, userId: string): Promise<Vehicle> {
    const vehicle = await this.vehicleRepository.findOne({ where: { id } });
    if (!vehicle) {
      throw new NotFoundException(`Vehicle with id ${id} not found`);
    }
    if (vehicle.userId !== userId) {
      throw new ForbiddenException('You do not own this vehicle');
    }
    return vehicle;
  }

  async update(
    id: string,
    userId: string,
    dto: UpdateVehicleDto,
  ): Promise<Vehicle> {
    const vehicle = await this.findOne(id, userId);

    if (dto.licensePlate && dto.licensePlate !== vehicle.licensePlate) {
      const duplicate = await this.vehicleRepository.findOne({
        where: { licensePlate: dto.licensePlate },
      });
      if (duplicate) {
        throw new ConflictException(
          `Vehicle with license plate ${dto.licensePlate} is already registered`,
        );
      }
    }

    Object.assign(vehicle, dto);
    return this.vehicleRepository.save(vehicle);
  }

  async remove(id: string, userId: string): Promise<void> {
    const vehicle = await this.findOne(id, userId);
    await this.vehicleRepository.remove(vehicle);
  }

  async logEntry(
    id: string,
    userId: string,
    dto: EntryExitDto,
  ): Promise<EntryExitLog> {
    const vehicle = await this.findOne(id, userId);

    const log = this.entryExitLogRepository.create({
      vehicleId: vehicle.id,
      parkingSpaceId: dto.parkingSpaceId,
      eventType: VehicleEventType.ENTRY,
    });

    return this.entryExitLogRepository.save(log);
  }

  async logExit(
    id: string,
    userId: string,
    dto: EntryExitDto,
  ): Promise<EntryExitLog> {
    const vehicle = await this.findOne(id, userId);

    const log = this.entryExitLogRepository.create({
      vehicleId: vehicle.id,
      parkingSpaceId: dto.parkingSpaceId,
      eventType: VehicleEventType.EXIT,
    });

    return this.entryExitLogRepository.save(log);
  }
}
