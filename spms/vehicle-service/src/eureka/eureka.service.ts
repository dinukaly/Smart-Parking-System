import {
  Injectable,
  Logger,
  OnModuleDestroy,
  OnModuleInit,
} from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { Eureka } from 'eureka-js-client';

@Injectable()
export class EurekaService implements OnModuleInit, OnModuleDestroy {
  private readonly logger = new Logger(EurekaService.name);
  private client: Eureka;

  constructor(private readonly configService: ConfigService) {}

  onModuleInit() {
    const port = Number(this.configService.get<number>('PORT', 3001));
    const appName = this.configService.get<string>(
      'EUREKA_APP_NAME',
      'VEHICLE-SERVICE',
    );
    const eurekaHost = this.configService.get<string>(
      'EUREKA_HOST',
      'localhost',
    );
    const eurekaPort = Number(
      this.configService.get<number>('EUREKA_PORT', 8761),
    );
    const servicePath = this.configService.get<string>(
      'EUREKA_SERVICE_PATH',
      '/eureka/apps/',
    );

    this.client = new Eureka({
      instance: {
        app: appName,
        hostName: 'localhost',
        ipAddr: '127.0.0.1',
        statusPageUrl: `http://localhost:${port}/api-docs`,
        healthCheckUrl: `http://localhost:${port}/health`,
        port: {
          $: port,
          '@enabled': true,
        },
        vipAddress: appName.toLowerCase(),
        dataCenterInfo: {
          '@class': 'com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo',
          name: 'MyOwn',
        },
      },
      eureka: {
        host: eurekaHost,
        port: eurekaPort,
        servicePath: servicePath,
        maxRetries: 10,
        requestRetryDelay: 2000,
      },
    });

    this.client.start((error: Error) => {
      if (error) {
        this.logger.warn(
          `Failed to register with Eureka Discovery Server: ${error.message}`,
        );
      } else {
        this.logger.log(
          `Successfully registered with Eureka Server at http://${eurekaHost}:${eurekaPort}${servicePath}`,
        );
      }
    });
  }

  onModuleDestroy() {
    if (this.client) {
      this.client.stop((error: Error) => {
        if (error) {
          this.logger.error(`Error stopping Eureka client: ${error.message}`);
        } else {
          this.logger.log('Deregistered from Eureka Discovery Server');
        }
      });
    }
  }
}
