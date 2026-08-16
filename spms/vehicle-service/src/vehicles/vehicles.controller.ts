import {
  Body,
  Controller,
  Delete,
  Get,
  HttpCode,
  HttpStatus,
  Param,
  ParseUUIDPipe,
  Post,
  Put,
  Req,
  UseGuards,
} from '@nestjs/common';
import {
  ApiBearerAuth,
  ApiCreatedResponse,
  ApiNoContentResponse,
  ApiOkResponse,
  ApiOperation,
  ApiTags,
} from '@nestjs/swagger';
import { Request } from 'express';
import { JwtGuard, JwtPayload } from '../auth/jwt.guard';
import { CreateVehicleDto } from './dto/create-vehicle.dto';
import { EntryExitDto } from './dto/entry-exit.dto';
import { UpdateVehicleDto } from './dto/update-vehicle.dto';
import { EntryExitLog } from './entities/entry-exit-log.entity';
import { Vehicle } from './entities/vehicle.entity';
import { VehiclesService } from './vehicles.service';

@ApiTags('Vehicles')
@ApiBearerAuth('bearerAuth')
@UseGuards(JwtGuard)
@Controller('api/vehicles')
export class VehiclesController {
  constructor(private readonly vehiclesService: VehiclesService) {}

  @Post()
  @ApiOperation({
    summary: 'Register a new vehicle',
    description: 'Creates a new vehicle owned by the authenticated user',
  })
  @ApiCreatedResponse({
    description: 'Vehicle registered successfully',
    type: Vehicle,
  })
  async create(
    @Req() req: Request,
    @Body() dto: CreateVehicleDto,
  ): Promise<Vehicle> {
    const user = (req as any).user as JwtPayload;
    return this.vehiclesService.create(user.sub, dto);
  }

  @Get()
  @ApiOperation({
    summary: "List current user's vehicles",
    description: 'Returns all vehicles owned by the authenticated user',
  })
  @ApiOkResponse({ description: 'List of vehicles', type: [Vehicle] })
  async findAll(@Req() req: Request): Promise<Vehicle[]> {
    const user = (req as any).user as JwtPayload;
    return this.vehiclesService.findAllByUser(user.sub);
  }

  @Get(':id')
  @ApiOperation({
    summary: 'Get vehicle by ID',
    description: 'Returns vehicle details for the given ID',
  })
  @ApiOkResponse({ description: 'Vehicle details', type: Vehicle })
  async findOne(
    @Req() req: Request,
    @Param('id', ParseUUIDPipe) id: string,
  ): Promise<Vehicle> {
    const user = (req as any).user as JwtPayload;
    return this.vehiclesService.findOne(id, user.sub);
  }

  @Put(':id')
  @ApiOperation({
    summary: 'Update vehicle details',
    description: "Updates vehicle fields for the authenticated user's vehicle",
  })
  @ApiOkResponse({ description: 'Updated vehicle', type: Vehicle })
  async update(
    @Req() req: Request,
    @Param('id', ParseUUIDPipe) id: string,
    @Body() dto: UpdateVehicleDto,
  ): Promise<Vehicle> {
    const user = (req as any).user as JwtPayload;
    return this.vehiclesService.update(id, user.sub, dto);
  }

  @Delete(':id')
  @HttpCode(HttpStatus.NO_CONTENT)
  @ApiOperation({
    summary: 'Remove a vehicle',
    description: 'Deletes a vehicle owned by the authenticated user',
  })
  @ApiNoContentResponse({ description: 'Vehicle deleted' })
  async remove(
    @Req() req: Request,
    @Param('id', ParseUUIDPipe) id: string,
  ): Promise<void> {
    const user = (req as any).user as JwtPayload;
    return this.vehiclesService.remove(id, user.sub);
  }

  @Post(':id/entry')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({
    summary: 'Simulate vehicle entry',
    description:
      'Logs an ENTRY event for a vehicle entering a parking space (IoT simulation)',
  })
  @ApiOkResponse({ description: 'Entry event logged', type: EntryExitLog })
  async logEntry(
    @Req() req: Request,
    @Param('id', ParseUUIDPipe) id: string,
    @Body() dto: EntryExitDto,
  ): Promise<EntryExitLog> {
    const user = (req as any).user as JwtPayload;
    return this.vehiclesService.logEntry(id, user.sub, dto);
  }

  @Post(':id/exit')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({
    summary: 'Simulate vehicle exit',
    description:
      'Logs an EXIT event for a vehicle leaving a parking space (IoT simulation)',
  })
  @ApiOkResponse({ description: 'Exit event logged', type: EntryExitLog })
  async logExit(
    @Req() req: Request,
    @Param('id', ParseUUIDPipe) id: string,
    @Body() dto: EntryExitDto,
  ): Promise<EntryExitLog> {
    const user = (req as any).user as JwtPayload;
    return this.vehiclesService.logExit(id, user.sub, dto);
  }
}
