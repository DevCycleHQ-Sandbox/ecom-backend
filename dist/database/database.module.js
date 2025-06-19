"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.DatabaseModule = void 0;
const common_1 = require("@nestjs/common");
const typeorm_1 = require("@nestjs/typeorm");
const config_1 = require("@nestjs/config");
const user_entity_1 = require("../entities/user.entity");
const product_entity_1 = require("../entities/product.entity");
const cart_item_entity_1 = require("../entities/cart-item.entity");
const order_entity_1 = require("../entities/order.entity");
const order_item_entity_1 = require("../entities/order-item.entity");
let DatabaseModule = class DatabaseModule {
};
exports.DatabaseModule = DatabaseModule;
exports.DatabaseModule = DatabaseModule = __decorate([
    (0, common_1.Module)({
        imports: [
            typeorm_1.TypeOrmModule.forRootAsync({
                imports: [config_1.ConfigModule],
                useFactory: (configService) => {
                    const isProduction = configService.get("NODE_ENV") === "production";
                    const usePostgres = configService.get("DATABASE_TYPE") === "postgres";
                    const baseConfig = {
                        entities: [user_entity_1.User, product_entity_1.Product, cart_item_entity_1.CartItem, order_entity_1.Order, order_item_entity_1.OrderItem],
                        synchronize: !isProduction,
                        logging: !isProduction,
                    };
                    if (usePostgres) {
                        return {
                            ...baseConfig,
                            type: "postgres",
                            url: configService.get("POSTGRES_URL"),
                        };
                    }
                    return {
                        ...baseConfig,
                        type: "sqlite",
                        database: configService.get("DATABASE_URL", "./database.sqlite"),
                    };
                },
                inject: [config_1.ConfigService],
            }),
        ],
    })
], DatabaseModule);
//# sourceMappingURL=database.module.js.map