import { ConfigService } from "@nestjs/config";
export declare class FeatureFlagService {
    private configService;
    private featureFlagClient;
    constructor(configService: ConfigService);
    private initializeOpenFeature;
    getBooleanValue(userId: string, key: string, defaultValue?: boolean): Promise<boolean>;
    getStringValue(userId: string, key: string, defaultValue?: string): Promise<string>;
    getNumberValue(userId: string, key: string, defaultValue?: number): Promise<number>;
    getObjectValue(userId: string, key: string, defaultValue?: any): Promise<any>;
    getVariableValue(userId: string, key: string, defaultValue: any): Promise<any>;
}
