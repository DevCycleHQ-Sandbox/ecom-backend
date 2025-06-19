import { DVCClient } from "@devcycle/nodejs-server-sdk";
export declare function initializeDevCycle(): void;
export declare function getDevCycleClient(): DVCClient | null;
export declare function getFeatureFlag(userId: string, flagKey: string, defaultValue?: boolean): Promise<boolean>;
