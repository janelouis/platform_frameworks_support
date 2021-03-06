/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Embedded
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.arch.persistence.room.Query
import android.arch.persistence.room.Relation
import android.arch.persistence.room.ext.LifecyclesTypeNames
import android.arch.persistence.room.ext.PagingTypeNames
import android.arch.persistence.room.ext.ReactiveStreamsTypeNames
import android.arch.persistence.room.ext.RoomRxJava2TypeNames
import android.arch.persistence.room.ext.RxJava2TypeNames
import android.arch.persistence.room.processor.EntityProcessor
import android.arch.persistence.room.solver.CodeGenScope
import android.arch.persistence.room.testing.TestInvocation
import android.arch.persistence.room.testing.TestProcessor
import android.arch.persistence.room.verifier.DatabaseVerifier
import android.arch.persistence.room.writer.ClassWriter
import com.google.auto.common.MoreElements
import com.google.common.truth.Truth
import com.google.testing.compile.CompileTester
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourcesSubjectFactory
import com.squareup.javapoet.ClassName
import org.mockito.Mockito
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import java.io.File
import javax.lang.model.element.Element
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.tools.JavaFileObject

object COMMON {
    val USER by lazy {
        loadJavaCode("common/input/User.java", "foo.bar.User")
    }
    val USER_TYPE_NAME by lazy {
        ClassName.get("foo.bar", "User")
    }
    val BOOK by lazy {
        loadJavaCode("common/input/Book.java", "foo.bar.Book")
    }
    val NOT_AN_ENTITY by lazy {
        loadJavaCode("common/input/NotAnEntity.java", "foo.bar.NotAnEntity")
    }

    val NOT_AN_ENTITY_TYPE_NAME by lazy {
        ClassName.get("foo.bar", "NotAnEntity")
    }

    val MULTI_PKEY_ENTITY by lazy {
        loadJavaCode("common/input/MultiPKeyEntity.java", "MultiPKeyEntity")
    }
    val LIVE_DATA by lazy {
        loadJavaCode("common/input/LiveData.java", LifecyclesTypeNames.LIVE_DATA.toString())
    }
    val COMPUTABLE_LIVE_DATA by lazy {
        loadJavaCode("common/input/ComputableLiveData.java",
                LifecyclesTypeNames.COMPUTABLE_LIVE_DATA.toString())
    }
    val PUBLISHER by lazy {
        loadJavaCode("common/input/reactivestreams/Publisher.java",
                ReactiveStreamsTypeNames.PUBLISHER.toString())
    }
    val FLOWABLE by lazy {
        loadJavaCode("common/input/rxjava2/Flowable.java", RxJava2TypeNames.FLOWABLE.toString())
    }

    val RX2_ROOM by lazy {
        loadJavaCode("common/input/Rx2Room.java", RoomRxJava2TypeNames.RX_ROOM.toString())
    }

    val LIVE_LAZY_LIST_PROVIDER by lazy {
        loadJavaCode("common/input/LiveLazyListProvider.java",
                PagingTypeNames.LIVE_LAZY_LIST_PROVIDER.toString())
    }
}
fun testCodeGenScope(): CodeGenScope {
    return CodeGenScope(Mockito.mock(ClassWriter::class.java))
}

fun simpleRun(vararg jfos : JavaFileObject, f: (TestInvocation) -> Unit): CompileTester {
    return Truth.assertAbout(JavaSourcesSubjectFactory.javaSources())
            .that(jfos.toList() + JavaFileObjects.forSourceString("foo.bar.MyClass",
                    """
                    package foo.bar;
                    abstract public class MyClass {
                    @android.arch.persistence.room.Query("foo")
                    abstract public void setFoo(String foo);
                    }
                    """))
            .processedWith(TestProcessor.builder()
                    .nextRunHandler {
                        f(it)
                        true
                    }
                    .forAnnotations(Query::class, PrimaryKey::class, Embedded::class,
                            ColumnInfo::class, Relation::class, Entity::class)
                    .build())
}

fun loadJavaCode(fileName : String, qName : String) : JavaFileObject {
    val contents = File("src/test/data/$fileName").readText(Charsets.UTF_8)
    return JavaFileObjects.forSourceString(qName, contents)
}

fun createVerifierFromEntities(invocation: TestInvocation) : DatabaseVerifier {
    val entities = invocation.roundEnv.getElementsAnnotatedWith(Entity::class.java).map {
        EntityProcessor(invocation.context, MoreElements.asType(it)).process()
    }
    return DatabaseVerifier.create(invocation.context, Mockito.mock(Element::class.java),
            entities)!!
}

/**
 * Create mocks of [Element] and [TypeMirror] so that they can be used for instantiating a fake
 * [android.arch.persistence.room.vo.Field].
 */
fun mockElementAndType(): Pair<Element, TypeMirror> {
    val element = mock(Element::class.java)
    val type = mock(TypeMirror::class.java)
    doReturn(TypeKind.DECLARED).`when`(type).kind
    doReturn(type).`when`(element).asType()
    return element to type
}
