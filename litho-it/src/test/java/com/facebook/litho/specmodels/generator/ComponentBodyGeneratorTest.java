/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.specmodels.generator;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.support.annotation.Nullable;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.Row;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.OnUpdateStateWithTransition;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.PropDefault;
import com.facebook.litho.annotations.State;
import com.facebook.litho.annotations.TreeProp;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.internal.RunMode;
import com.facebook.litho.specmodels.model.ClassNames;
import com.facebook.litho.specmodels.model.EventDeclarationModel;
import com.facebook.litho.specmodels.model.PropModel;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.model.SpecModelUtils;
import com.facebook.litho.specmodels.model.StateParamModel;
import com.facebook.litho.specmodels.model.TypeSpec;
import com.facebook.litho.specmodels.model.TypeSpec.DeclaredTypeSpec;
import com.facebook.litho.specmodels.processor.LayoutSpecModelFactory;
import com.google.testing.compile.CompilationRule;
import com.squareup.javapoet.ClassName;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/** Tests {@link ComponentBodyGenerator} */
public class ComponentBodyGeneratorTest {
  @Rule public CompilationRule mCompilationRule = new CompilationRule();
  @Mock private Messager mMessager;

  private final LayoutSpecModelFactory mLayoutSpecModelFactory = new LayoutSpecModelFactory();

  @LayoutSpec
  static class TestSpec {
    @PropDefault protected static boolean arg0 = true;

    @OnCreateLayout
    public void testDelegateMethod(
        @Prop boolean arg0,
        @State int arg1,
        @Param Object arg2,
        @TreeProp long arg3,
        @Prop @Nullable Component arg4,
        @Prop List<Component> arg5,
        @Prop List<String> arg6,
        @TreeProp Set<List<Row>> arg7,
        @TreeProp Set<Integer> arg8) {}

    @OnEvent(Object.class)
    public void testEventMethod(
        @Prop boolean arg0,
        @State int arg1,
        @Param Object arg2,
        @TreeProp long arg3,
        @Prop @Nullable Component arg4) {}

    @OnUpdateState
    public void testUpdateStateMethod() {}
  }

  @LayoutSpec
  static class TestWithTransitionSpec {
    @PropDefault protected static boolean arg0 = true;

    @OnCreateLayout
    public void testDelegateMethod(
        @Prop boolean arg0,
        @State int arg1,
        @Param Object arg2,
        @TreeProp long arg3,
        @Prop Component arg4,
        @Prop List<Component> arg5,
        @Prop List<String> arg6,
        @TreeProp Set<List<Row>> arg7,
        @TreeProp Set<Integer> arg8) {}

    @OnUpdateState
    public void testUpdateStateMethod() {}

    @OnUpdateStateWithTransition
    public void testUpdateStateWithTransitionMethod() {}
  }

  @LayoutSpec
  static class TestKotlinWildcardsSpec {
    public static final TestKotlinWildcardsSpec INSTANCE = null;

    @OnCreateLayout
    public final Component onCreateLayout(
        ComponentContext c,
        @Prop(varArg = "number") java.util.List<? extends java.lang.Number> numbers) {
      return null;
    }
  }

  private SpecModel mSpecModelDI;
  private SpecModel mSpecModelWithTransitionDI;
  private SpecModel mKotlinWildcardsSpecModel;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    Elements elements = mCompilationRule.getElements();
    Types types = mCompilationRule.getTypes();
    TypeElement typeElement = elements.getTypeElement(TestSpec.class.getCanonicalName());
    mSpecModelDI =
        mLayoutSpecModelFactory.create(
            elements, types, typeElement, mMessager, RunMode.NORMAL, null, null);

    TypeElement typeElementWithTransition =
        elements.getTypeElement(TestWithTransitionSpec.class.getCanonicalName());
    mSpecModelWithTransitionDI =
        mLayoutSpecModelFactory.create(
            elements, types, typeElementWithTransition, mMessager, RunMode.NORMAL, null, null);

    TypeElement typeElementKotlinVarArgsWildcards =
        elements.getTypeElement(TestKotlinWildcardsSpec.class.getCanonicalName());
    mKotlinWildcardsSpecModel =
        mLayoutSpecModelFactory.create(
            elements,
            types,
            typeElementKotlinVarArgsWildcards,
            mMessager,
            RunMode.NORMAL,
            null,
            null);
  }

  @Test
  public void testGenerateStateContainerImpl() {
    assertThat(ComponentBodyGenerator.generateStateContainer(mSpecModelDI).toString())
        .isEqualTo(
            "@android.support.annotation.VisibleForTesting(\n"
                + "    otherwise = 2\n"
                + ")\n"
                + "static class TestStateContainer implements com.facebook.litho.StateContainer {\n"
                + "  @com.facebook.litho.annotations.State\n"
                + "  int arg1;\n"
                + "}\n");
  }

  @Test
  public void testGenerateStateContainerWithTransitionImpl() {
    assertThat(ComponentBodyGenerator.generateStateContainer(mSpecModelWithTransitionDI).toString())
        .isEqualTo(
            "@android.support.annotation.VisibleForTesting(\n"
                + "    otherwise = 2\n"
                + ")\n"
                + "static class TestWithTransitionStateContainer implements com.facebook.litho.StateContainer, "
                + "com.facebook.litho.ComponentLifecycle.TransitionContainer {\n"
                + "  @com.facebook.litho.annotations.State\n"
                + "  int arg1;\n"
                + "\n"
                + "  java.util.List<com.facebook.litho.Transition> _transitions = new java.util.ArrayList<>();\n"
                + "\n"
                + "  @java.lang.Override\n"
                + "  public java.util.List<com.facebook.litho.Transition> consumeTransitions() {\n"
                + "    if (_transitions.isEmpty()) {\n"
                + "      return java.util.Collections.EMPTY_LIST;\n"
                + "    }\n"
                + "    java.util.List<com.facebook.litho.Transition> transitionsCopy;\n"
                + "    synchronized (_transitions) {\n"
                + "      transitionsCopy = new java.util.ArrayList<>(_transitions);\n"
                + "      _transitions.clear();\n"
                + "    }\n"
                + "    return transitionsCopy;\n"
                + "  }\n"
                + "}\n");
  }

  @Test
  public void testGetStateContainerClassName() {
    assertThat(ComponentBodyGenerator.getStateContainerClassName(mSpecModelDI))
        .isEqualTo("TestStateContainer");
  }

  @Test
  public void testGenerateStateContainerGetter() {
    assertThat(
            ComponentBodyGenerator.generateStateContainerGetter(ClassNames.STATE_CONTAINER)
                .toString())
        .isEqualTo(
            "@java.lang.Override\n"
                + "protected com.facebook.litho.StateContainer getStateContainer() {\n"
                + "  return mStateContainer;\n"
                + "}\n");
  }

  @Test
  public void testGenerateProps() {
    TypeSpecDataHolder dataHolder = ComponentBodyGenerator.generateProps(mSpecModelDI);
    assertThat(dataHolder.getFieldSpecs()).hasSize(4);
    assertThat(dataHolder.getFieldSpecs().get(0).toString())
        .isEqualTo(
            "@com.facebook.litho.annotations.Prop(\n"
                + "    resType = com.facebook.litho.annotations.ResType.NONE,\n"
                + "    optional = false\n"
                + ")\n"
                + "boolean arg0 = TestSpec.arg0;\n");
    assertThat(dataHolder.getFieldSpecs().get(1).toString())
        .isEqualTo(
            "@android.support.annotation.Nullable\n"
                + "@com.facebook.litho.annotations.Prop(\n"
                + "    resType = com.facebook.litho.annotations.ResType.NONE,\n"
                + "    optional = false\n"
                + ")\n"
                + "com.facebook.litho.Component arg4;\n");
  }

  @Test
  public void testGeneratePropsForKotlinWildcards() {
    TypeSpecDataHolder dataHolder = ComponentBodyGenerator.generateProps(mKotlinWildcardsSpecModel);
    assertThat(dataHolder.getFieldSpecs()).hasSize(1);
    assertThat(dataHolder.getFieldSpecs().get(0).toString())
        .isEqualTo(
            "@com.facebook.litho.annotations.Prop(\n"
                + "    resType = com.facebook.litho.annotations.ResType.NONE,\n"
                + "    optional = false\n"
                + ")\n"
                + "java.util.List<java.lang.Number> numbers;\n");
  }

  @Test
  public void testGenerateTreeProps() {
    TypeSpecDataHolder dataHolder = ComponentBodyGenerator.generateTreeProps(mSpecModelDI);
    assertThat(dataHolder.getFieldSpecs()).hasSize(3);
    assertThat(dataHolder.getFieldSpecs().get(0).toString())
        .isEqualTo("@com.facebook.litho.annotations.TreeProp\nlong arg3;\n");
  }

  @Test
  public void testGenerateInterStageInputs() {
    TypeSpecDataHolder dataHolder = ComponentBodyGenerator.generateInterStageInputs(mSpecModelDI);
    assertThat(dataHolder.getFieldSpecs()).hasSize(0);
  }

  @Test
  public void testGenerateEventDeclarations() {
    SpecModel specModel = mock(SpecModel.class);
    when(specModel.getEventDeclarations())
        .thenReturn(
            ImmutableList.of(
                new EventDeclarationModel(
                    ClassName.OBJECT,
                    ClassName.OBJECT,
                    ImmutableList.<EventDeclarationModel.FieldModel>of(),
                    null)));

    TypeSpecDataHolder dataHolder = ComponentBodyGenerator.generateEventHandlers(specModel);
    assertThat(dataHolder.getFieldSpecs()).hasSize(1);
    assertThat(dataHolder.getFieldSpecs().get(0).toString())
        .isEqualTo("com.facebook.litho.EventHandler objectHandler;\n");
  }

  @Test
  public void testGenerateIsEquivalentMethod() {
    assertThat(ComponentBodyGenerator.generateIsEquivalentMethod(mSpecModelDI).toString())
        .isEqualTo(
            "@java.lang.Override\n"
                + "public boolean isEquivalentTo(com.facebook.litho.Component other) {\n"
                + "  if (com.facebook.litho.config.ComponentsConfiguration.useNewIsEquivalentTo) {\n"
                + "    return super.isEquivalentTo(other);\n"
                + "  }\n"
                + "  if (this == other) {\n"
                + "    return true;\n"
                + "  }\n"
                + "  if (other == null || getClass() != other.getClass()) {\n"
                + "    return false;\n"
                + "  }\n"
                + "  Test testRef = (Test) other;\n"
                + "  if (this.getId() == testRef.getId()) {\n"
                + "    return true;\n"
                + "  }\n"
                + "  if (arg0 != testRef.arg0) {\n"
                + "    return false;\n"
                + "  }\n"
                + "  if (arg4 != null ? !arg4.isEquivalentTo(testRef.arg4) : testRef.arg4 != null) {\n"
                + "    return false;\n"
                + "  }\n"
                + "  if (arg5 != null) {\n"
                + "    if (testRef.arg5 == null || arg5.size() != testRef.arg5.size()) {\n"
                + "      return false;\n"
                + "    }\n"
                + "    java.util.Iterator<com.facebook.litho.Component> _e1_1 = arg5.iterator();\n"
                + "    java.util.Iterator<com.facebook.litho.Component> _e2_1 = testRef.arg5.iterator();\n"
                + "    while (_e1_1.hasNext() && _e2_1.hasNext()) {\n"
                + "      if (!_e1_1.next().isEquivalentTo(_e2_1.next())) {\n"
                + "        return false;\n"
                + "      }\n"
                + "    }\n"
                + "  } else if (testRef.arg5 != null) {\n"
                + "    return false;\n"
                + "  }\n"
                + "  if (arg6 != null ? !arg6.equals(testRef.arg6) : testRef.arg6 != null) {\n"
                + "    return false;\n"
                + "  }\n"
                + "  if (mStateContainer.arg1 != testRef.mStateContainer.arg1) {\n"
                + "    return false;\n"
                + "  }\n"
                + "  if (arg3 != testRef.arg3) {\n"
                + "    return false;\n"
                + "  }\n"
                + "  if (arg7 != null) {\n"
                + "    if (testRef.arg7 == null || arg7.size() != testRef.arg7.size()) {\n"
                + "      return false;\n"
                + "    }\n"
                + "    java.util.Iterator<java.util.List<com.facebook.litho.Row>> _e1_2 = arg7.iterator();\n"
                + "    java.util.Iterator<java.util.List<com.facebook.litho.Row>> _e2_2 = testRef.arg7.iterator();\n"
                + "    while (_e1_2.hasNext() && _e2_2.hasNext()) {\n"
                + "      if (_e1_2.next().size() != _e2_2.next().size()) {\n"
                + "        return false;\n"
                + "      }\n"
                + "      java.util.Iterator<com.facebook.litho.Row> _e1_1 = _e1_2.next().iterator();\n"
                + "      java.util.Iterator<com.facebook.litho.Row> _e2_1 = _e2_2.next().iterator();\n"
                + "      while (_e1_1.hasNext() && _e2_1.hasNext()) {\n"
                + "        if (!_e1_1.next().isEquivalentTo(_e2_1.next())) {\n"
                + "          return false;\n"
                + "        }\n"
                + "      }\n"
                + "    }\n"
                + "  } else if (testRef.arg7 != null) {\n"
                + "    return false;\n"
                + "  }\n"
                + "  if (arg8 != null ? !arg8.equals(testRef.arg8) : testRef.arg8 != null) {\n"
                + "    return false;\n"
                + "  }\n"
                + "  return true;\n"
                + "}\n");
  }

  @Test
  public void testOnUpdateStateMethods() {
    TypeSpecDataHolder dataHolder =
        ComponentBodyGenerator.generateOnUpdateStateMethods(mSpecModelDI);
    assertThat(dataHolder.getMethodSpecs()).hasSize(1);
    assertThat(dataHolder.getMethodSpecs().get(0).toString())
        .isEqualTo(
            "private TestUpdateStateMethodStateUpdate createTestUpdateStateMethodStateUpdate() {\n"
                + "  return new TestUpdateStateMethodStateUpdate();\n"
                + "}\n");
  }

  @Test
  public void testOnUpdateStateWithTransitionMethods() {
    TypeSpecDataHolder dataHolder =
        ComponentBodyGenerator.generateOnUpdateStateMethods(mSpecModelWithTransitionDI);
    assertThat(dataHolder.getMethodSpecs()).hasSize(2);
    assertThat(dataHolder.getMethodSpecs().get(0).toString())
        .isEqualTo(
            "private TestUpdateStateMethodStateUpdate createTestUpdateStateMethodStateUpdate() {\n"
                + "  return new TestUpdateStateMethodStateUpdate();\n"
                + "}\n");
    assertThat(dataHolder.getMethodSpecs().get(1).toString())
        .isEqualTo(
            "private TestUpdateStateWithTransitionMethodStateUpdate createTestUpdateStateWithTransitionMethodStateUpdate() {\n"
                + "  return new TestUpdateStateWithTransitionMethodStateUpdate();\n"
                + "}\n");
  }

  @Test
  public void testGenerateStateParamImplAccessor() {
    StateParamModel stateParamModel = mock(StateParamModel.class);
    when(stateParamModel.getName()).thenReturn("stateParam");
    assertThat(ComponentBodyGenerator.getImplAccessor(mSpecModelDI, stateParamModel))
        .isEqualTo("mStateContainer.stateParam");
  }

  @Test
  public void testGeneratePropParamImplAccessor() {
    PropModel propModel = mock(PropModel.class);
    when(propModel.getName()).thenReturn("propParam");
    assertThat(ComponentBodyGenerator.getImplAccessor(mSpecModelDI, propModel))
        .isEqualTo("propParam");
  }

  @Test
  public void testCalculateLevelOfComponentInCollections() {
    Elements elements = mCompilationRule.getElements();
    TypeElement typeElement = elements.getTypeElement(CollectionObject.class.getCanonicalName());
    List<? extends Element> fields = typeElement.getEnclosedElements();
    TypeSpec arg0 = SpecModelUtils.generateTypeSpec(fields.get(0).asType());
    TypeSpec arg1 = SpecModelUtils.generateTypeSpec(fields.get(1).asType());
    TypeSpec arg2 = SpecModelUtils.generateTypeSpec(fields.get(2).asType());

    assertThat(arg0.getClass()).isEqualTo(DeclaredTypeSpec.class);
    assertThat(arg1.getClass()).isEqualTo(DeclaredTypeSpec.class);
    assertThat(arg2.getClass()).isEqualTo(DeclaredTypeSpec.class);
    assertThat(
            ComponentBodyGenerator.calculateLevelOfComponentInCollections((DeclaredTypeSpec) arg0))
        .isEqualTo(1);
    assertThat(
            ComponentBodyGenerator.calculateLevelOfComponentInCollections((DeclaredTypeSpec) arg1))
        .isEqualTo(2);
    assertThat(
            ComponentBodyGenerator.calculateLevelOfComponentInCollections((DeclaredTypeSpec) arg2))
        .isEqualTo(0);
  }

  private static class CollectionObject {
    List<Component> arg0;
    List<Set<Component>> arg1;
    Set<List<List<Integer>>> arg2;
  }
}
