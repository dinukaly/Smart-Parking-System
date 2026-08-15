import {
  Column,
  CreateDateColumn,
  Entity,
  Index,
  OneToMany,
  PrimaryGeneratedColumn,
  UpdateDateColumn,
} from 'typeorm';
import { EntryExitLog } from './entry-exit-log.entity';

export enum VehicleType {
  CAR = 'CAR',
  MOTORCYCLE = 'MOTORCYCLE',
  TRUCK = 'TRUCK',
}

@Entity('vehicles')
@Index('idx_vehicles_user_id', ['userId'])
@Index('idx_vehicles_license_plate', ['licensePlate'], { unique: true })
export class Vehicle {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  /**
   * Logical FK to user-service.users.id — no DB-level constraint
   * since User Service runs in a separate database (spms_users).
   * Consistency is enforced at the application layer via JWT claim.
   */
  @Column({ name: 'user_id', type: 'uuid', nullable: false })
  userId: string;

  @Column({
    name: 'license_plate',
    type: 'varchar',
    length: 20,
    unique: true,
    nullable: false,
  })
  licensePlate: string;

  @Column({ type: 'varchar', length: 100, nullable: true })
  make: string;

  @Column({ type: 'varchar', length: 100, nullable: true })
  model: string;

  @Column({ type: 'varchar', length: 50, nullable: true })
  color: string;

  @Column({
    name: 'vehicle_type',
    type: 'enum',
    enum: VehicleType,
    default: VehicleType.CAR,
  })
  vehicleType: VehicleType;

  @CreateDateColumn({ name: 'created_at', type: 'timestamptz' })
  createdAt: Date;

  @UpdateDateColumn({ name: 'updated_at', type: 'timestamptz' })
  updatedAt: Date;

  @OneToMany(() => EntryExitLog, (log) => log.vehicle)
  entryExitLogs: EntryExitLog[];
}
