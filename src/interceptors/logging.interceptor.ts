import {
  CallHandler,
  ExecutionContext,
  Injectable,
  NestInterceptor,
} from "@nestjs/common"
import { Observable } from "rxjs"
import { tap } from "rxjs/operators"

@Injectable()
export class LoggingInterceptor implements NestInterceptor {
  intercept(context: ExecutionContext, next: CallHandler): Observable<any> {
    const request = context.switchToHttp().getRequest()
    const response = context.switchToHttp().getResponse()

    const { method, url, user } = request
    const userInfo = user
      ? `[User: ${user.username || user.id || "unknown"}]`
      : "[Anonymous]"
    const startTime = Date.now()

    console.log(`🔄 ${method} ${url} ${userInfo} - Request started`)

    return next.handle().pipe(
      tap({
        next: () => {
          const duration = Date.now() - startTime
          const statusCode = response.statusCode
          console.log(
            `✅ ${method} ${url} ${userInfo} - ${statusCode} (${duration}ms)`
          )
        },
        error: (error) => {
          const duration = Date.now() - startTime
          const statusCode = error.status || 500
          console.log(
            `❌ ${method} ${url} ${userInfo} - ${statusCode} (${duration}ms) [${error.message}]`
          )
        },
      })
    )
  }
}
