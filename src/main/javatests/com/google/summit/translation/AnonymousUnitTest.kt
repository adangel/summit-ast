/*
 * Copyright 2026 Andreas Dangel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.summit.translation

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.google.summit.ast.AnonymousUnit
import com.google.summit.ast.declaration.ClassDeclaration
import com.google.summit.ast.declaration.Declaration
import com.google.summit.ast.declaration.EnumDeclaration
import com.google.summit.ast.declaration.FieldDeclarationGroup
import com.google.summit.ast.declaration.InterfaceDeclaration
import com.google.summit.ast.declaration.MethodDeclaration
import com.google.summit.ast.declaration.PropertyDeclaration
import com.google.summit.ast.modifier.KeywordModifier
import com.google.summit.ast.statement.Statement
import com.google.summit.testing.TranslateHelpers
import kotlin.test.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class AnonymousUnitTest {

    @Test
    fun empty() {
        val cu = TranslateHelpers.parseAndTranslateWithExceptions("", "anonymousUnit")

        assertThat(cu.typeDeclaration).isNull()
        assertThat(cu.anonymousUnit).isInstanceOf(AnonymousUnit::class.java)
    }

    @Test
    fun statement() {
        val cu = TranslateHelpers.parseAndTranslateWithExceptions("System.debug('');", "anonymousUnit")

        assertThat(cu.typeDeclaration).isNull()
        assertThat(cu.anonymousUnit).isInstanceOf(AnonymousUnit::class.java)
        assertThat(cu.anonymousUnit!!.body[0]).isInstanceOf(Statement::class.java)
    }

    @Test
    fun method() {
        val cu = TranslateHelpers.parseAndTranslateWithExceptions("public void func() {}", "anonymousUnit")

        assertThat(cu.typeDeclaration).isNull()
        assertThat(cu.anonymousUnit).isInstanceOf(AnonymousUnit::class.java)
        assertThat(cu.anonymousUnit!!.body[0]).isInstanceOf(MethodDeclaration::class.java)
    }
}
