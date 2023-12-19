package de.sam.base.components

import de.sam.base.actions.FileCleanupAction
import de.sam.base.actions.SessionCleanupAction
import io.javalin.http.Context

class ActionsComponent {
    fun list(ctx: Context) {
        ctx.render("components/actionsList.kte")
    }

    fun runSingle(ctx: Context) {
        val param = ctx.pathParam("action")
        when (param) {
            "delete-dangling-files" -> {
                FileCleanupAction().cleanup()
            }

            "remove-all-sessions" -> {
                SessionCleanupAction().cleanup()
            }
        }

        ctx.redirect("/admin/actions")
    }
}