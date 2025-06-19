import { AuthService } from "./auth.service";
import { LoginDto } from "./dto/login.dto";
import { RegisterDto } from "./dto/register.dto";
export declare class AuthController {
    private authService;
    constructor(authService: AuthService);
    register(registerDto: RegisterDto): Promise<{
        message: string;
        token: string;
        user: {
            id: string;
            username: string;
            email: string;
            role: "user" | "admin";
        };
    }>;
    login(loginDto: LoginDto): Promise<{
        message: string;
        token: string;
        user: {
            id: string;
            username: string;
            email: string;
            role: "user" | "admin";
        };
    }>;
    verify(req: any): Promise<{
        message: string;
        user: {
            id: any;
            username: any;
            email: any;
            role: any;
        };
    }>;
}
