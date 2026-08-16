import { IsNotEmpty, IsString } from 'class-validator';
import { ApiProperty } from '@nestjs/swagger';

export class EntryExitDto {
  @ApiProperty({
    example: 'parking-space-uuid',
    description: 'ID of the parking space from Parking Service',
  })
  @IsString()
  @IsNotEmpty()
  parkingSpaceId: string;
}
