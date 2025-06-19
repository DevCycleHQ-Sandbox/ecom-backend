declare class OrderItemDto {
    product_id: string;
    quantity: number;
    price: number;
}
export declare class CreateOrderDto {
    shipping_address: string;
    card_number: string;
    items: OrderItemDto[];
}
export {};
