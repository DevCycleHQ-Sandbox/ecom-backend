"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
var __param = (this && this.__param) || function (paramIndex, decorator) {
    return function (target, key) { decorator(target, key, paramIndex); }
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.CartService = void 0;
const common_1 = require("@nestjs/common");
const typeorm_1 = require("@nestjs/typeorm");
const typeorm_2 = require("typeorm");
const cart_item_entity_1 = require("../entities/cart-item.entity");
const product_entity_1 = require("../entities/product.entity");
let CartService = class CartService {
    constructor(cartRepository, productRepository) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
    }
    async getCartItems(userId) {
        return await this.cartRepository.find({
            where: { user_id: userId },
            relations: ["product"],
        });
    }
    async addToCart(userId, productId, quantity) {
        const product = await this.productRepository.findOne({
            where: { id: productId },
        });
        if (!product) {
            throw new common_1.NotFoundException("Product not found");
        }
        const existingItem = await this.cartRepository.findOne({
            where: { user_id: userId, product_id: productId },
        });
        if (existingItem) {
            existingItem.quantity += quantity;
            return await this.cartRepository.save(existingItem);
        }
        const cartItem = this.cartRepository.create({
            user_id: userId,
            product_id: productId,
            quantity,
        });
        return await this.cartRepository.save(cartItem);
    }
    async updateCartItem(userId, cartItemId, quantity) {
        const cartItem = await this.cartRepository.findOne({
            where: { id: cartItemId, user_id: userId },
        });
        if (!cartItem) {
            throw new common_1.NotFoundException("Cart item not found");
        }
        cartItem.quantity = quantity;
        return await this.cartRepository.save(cartItem);
    }
    async removeFromCart(userId, cartItemId) {
        const result = await this.cartRepository.delete({
            id: cartItemId,
            user_id: userId,
        });
        if (result.affected === 0) {
            throw new common_1.NotFoundException("Cart item not found");
        }
    }
    async clearCart(userId) {
        await this.cartRepository.delete({ user_id: userId });
    }
};
exports.CartService = CartService;
exports.CartService = CartService = __decorate([
    (0, common_1.Injectable)(),
    __param(0, (0, typeorm_1.InjectRepository)(cart_item_entity_1.CartItem)),
    __param(1, (0, typeorm_1.InjectRepository)(product_entity_1.Product)),
    __metadata("design:paramtypes", [typeorm_2.Repository,
        typeorm_2.Repository])
], CartService);
//# sourceMappingURL=cart.service.js.map