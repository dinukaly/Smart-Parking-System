declare module 'eureka-js-client' {
  export class Eureka {
    constructor(config: any);
    start(callback?: (error: Error, ...rest: any[]) => void): void;
    stop(callback?: (error: Error, ...rest: any[]) => void): void;
    getInstancesByAppId(appId: string): any[];
  }
}
