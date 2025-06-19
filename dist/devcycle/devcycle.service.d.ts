import { ConfigService } from "@nestjs/config";
export declare class DevCycleService {
    private configService;
    private devcycleClient;
    constructor(configService: ConfigService);
    private initializeDevCycle;
    getVariableValue(userId: string, key: string, defaultValue: any): Promise<any>;
}
