import { Client } from "@openfeature/server-sdk";
export declare function initializeOpenFeature(): Promise<void>;
export declare function getFeatureFlagClient(): Client | null;
export declare function getFeatureFlag(userId: string, flagKey: string, defaultValue?: boolean): Promise<boolean>;
export declare function getStringFlag(userId: string, flagKey: string, defaultValue?: string): Promise<string>;
export declare function getNumberFlag(userId: string, flagKey: string, defaultValue?: number): Promise<number>;
export declare function getObjectFlag(userId: string, flagKey: string, defaultValue?: any): Promise<any>;
export declare const initializeDevCycle: typeof initializeOpenFeature;
export declare const getDevCycleClient: typeof getFeatureFlagClient;
