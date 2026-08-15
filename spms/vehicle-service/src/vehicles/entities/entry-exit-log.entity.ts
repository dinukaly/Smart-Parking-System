import {
  Column,
  CreateDateColumn,
  Entity,
  Index,
  JoinColumn,
  ManyToOne,
  PrimaryGeneratedColumn,
} from 'typeorm';
import { Vehicle } from './vehicle.entity';

export enum VehicleEventType {
  ENTRY = 'ENTRY',
  EXIT = 'EXIT',
}

@Entity('entry_exit_logs')
@Index('idx_entry_exit_logs_vehicle_id', ['vehicleId'])
export class EntryExitLog {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ name: 'vehicle_id', type: 'uuid', nullable: false })
  vehicleId: string;

  /**
   * Logical FK to parking-service.parking_spaces.id — no DB-level constraint
   * since Parking Service runs in a separate database (spms_parking).
   */
  @Column({
    name: 'parking_space_id',
    type: 'varchar',
    length: 255,
    nullable: false,
  })
  parkingSpaceId: string;

  @Column({
    name: 'event_type',
    type: 'enum',
    enum: VehicleEventType,
    nullable: false,
  })
  eventType: VehicleEventType;

  @CreateDateColumn({ name: 'timestamp', type: 'timestamptz' })
  timestamp: Date;

  @ManyToOne(() => Vehicle, (vehicle) => vehicle.entryExitLogs, {
    onDelete: 'CASCADE',
    nullable: false,
  })
  @JoinColumn({ name: 'vehicle_id' })
  vehicle: Vehicle;
}
