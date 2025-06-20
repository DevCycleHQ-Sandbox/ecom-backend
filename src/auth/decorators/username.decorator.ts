import { createParamDecorator, ExecutionContext } from "@nestjs/common"

export const Username = createParamDecorator(
  (data: unknown, ctx: ExecutionContext): string => {
    const request = ctx.switchToHttp().getRequest()
    const user = request.user

    if (!user) {
      // If no user is authenticated, return 'anonymous' for feature flag targeting
      return "anonymous"
    }

    // Return the username for feature flag targeting
    return user.username || "anonymous"
  }
)
