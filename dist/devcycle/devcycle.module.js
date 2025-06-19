"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.DevCycleModule = void 0;
const common_1 = require("@nestjs/common");
const devcycle_service_1 = require("./devcycle.service");
let DevCycleModule = class DevCycleModule {
};
exports.DevCycleModule = DevCycleModule;
exports.DevCycleModule = DevCycleModule = __decorate([
    (0, common_1.Module)({
        providers: [devcycle_service_1.DevCycleService],
        exports: [devcycle_service_1.DevCycleService],
    })
], DevCycleModule);
//# sourceMappingURL=devcycle.module.js.map