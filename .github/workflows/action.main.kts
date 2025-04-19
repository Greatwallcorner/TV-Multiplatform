#!/usr/bin/env kotlin

@file:Repository("https://repo.maven.apache.org/maven2/")
@file:DependsOn("io.github.typesafegithub:github-workflows-kt:3.3.0")
@file:Repository("https://bindings.krzeminski.it")
@file:DependsOn("actions:checkout:v4")
@file:DependsOn("dawidd6:action-get-tag:v1")



import io.github.typesafegithub.workflows.actions.actions.Checkout
import io.github.typesafegithub.workflows.domain.JobOutputs
import io.github.typesafegithub.workflows.domain.RunnerType.UbuntuLatest
import io.github.typesafegithub.workflows.domain.triggers.Push
import io.github.typesafegithub.workflows.dsl.expressions.Contexts
import io.github.typesafegithub.workflows.dsl.expressions.contexts.EnvContext.GITHUB_ENV
import io.github.typesafegithub.workflows.dsl.expressions.expr
import io.github.typesafegithub.workflows.dsl.workflow

workflow(
    name = "TV-Multiplatform Build Pre Release",
    on = listOf(Push()),
    sourceFile = __FILE__,
) {
    job(id = "set-tag", runsOn = UbuntuLatest, outputs = object: JobOutputs(){
        val tag by output()
        val lastTag by output()
    }) {
        uses(name = "Check out", action = Checkout(), _customArguments = mapOf(
            "fetch-depth" to 0
        ))

        run(
            name = "Set tag output",
            command = """
                echo "tag=$(date +"%Y%m%d%H%M%S")" >> $GITHUB_ENV
                echo "::set-output name=tag::$(date +"%Y%m%d%H%M%S")"
            """.trimIndent()
        )

        jobOutputs.tag = s


        
    }
}