package com.shoprunner.baleen.kapt

import com.shoprunner.baleen.ValidationError
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import java.lang.ClassCastException
import java.lang.NullPointerException
import javax.annotation.processing.Messager
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

internal class ExtraTestBuilder(
    private val elementUtils: Elements,
    private val typeUtils: Types,
    private val messager: Messager
) {

    fun addExtraTest(
        extraTest: DataTestElement,
        typeElement: TypeElement
    ): CodeBlock {
        return CodeBlock.builder().apply {
            add("/* ${extraTest.executableElement.annotationMirrors} ${extraTest.executableElement} */\n")
            beginControlFlow("it.test")
            add("dataTrace, data ->\n")
            // Create object
            indent()
            beginControlFlow("try")
            add("val obj = %M(data)\n", MemberName("com.shoprunner.baleen.kotlin", "as${typeElement.simpleName}"))

            val memberName = MemberName(
                elementUtils.getPackageOf(extraTest.executableElement).toString(),
                extraTest.executableElement.simpleName.toString()
            )

            if (extraTest.isExtension) {
                add("obj.%M(dataTrace).asSequence()", memberName)
            } else {
                add("%M(obj, dataTrace).asSequence()", memberName)
            }
            add("\n")
            endControlFlow()
            beginControlFlow("catch(npe: %T)", NullPointerException::class.java)
            add(
                "sequenceOf(%T(dataTrace, %P, data))\n",
                ValidationError::class.java,
                "Unable to run test `${memberName.canonicalName}`: '\${npe.message}'"
            )
            endControlFlow()
            beginControlFlow("catch(cce: %T)", ClassCastException::class.java)
            add(
                "sequenceOf(%T(dataTrace, %P, data))\n",
                ValidationError::class.java,
                "Unable to run test `${memberName.canonicalName}`: '\${cce.message}'"
            )
            endControlFlow()
            unindent()
            endControlFlow()
        }.build()
    }
}
