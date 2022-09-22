package com.nabla.sdk.core.ui.helpers

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.nabla.sdk.core.R
import com.nabla.sdk.core.annotation.NablaInternal
import com.nabla.sdk.core.data.exception.mapFailure
import com.nabla.sdk.core.domain.entity.InternalException.Companion.asNablaInternal
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Delegates a viewBinding read-only property storing it inside the bind view's tag.
 *
 * usage:
 * ```
 *  class ExampleFragment : Fragment {
 *      //...
 *      private val binding: ExampleFragmentBinding by viewBinding(ExampleFragmentBinding::bind)
 *      // ...
 *  }
 * ```
 *
 * @param ViewBindingT the [ViewBinding] property type.
 * @param bind You need to call [ViewBindingT].bind static method to bind the view binding
 *  to fragment's view.
 * @return a [ViewBindingT] delegates
 *
 */
@NablaInternal
public fun <ViewBindingT : ViewBinding> viewBinding(
    bind: (View) -> ViewBindingT
): ReadOnlyProperty<Fragment, ViewBindingT> {
    return FragmentViewBindingDelegate(bind)
}

private class FragmentViewBindingDelegate<out ViewBindingT : ViewBinding>(
    private val bind: (View) -> ViewBindingT
) : ReadOnlyProperty<Fragment, ViewBindingT> {
    override fun getValue(thisRef: Fragment, property: KProperty<*>): ViewBindingT {
        return thisRef.requireViewOrThrow().getOrPutBinding(R.id.nabla_view_binding_tag)
    }

    @Suppress("UNCHECKED_CAST")
    private fun View.getOrPutBinding(key: Int): ViewBindingT {
        val binding = getTag(key) as? ViewBindingT
        if (binding == null) {
            val newBinding = bind(this)
            setTag(key, newBinding)
            return newBinding
        }
        return binding
    }
}

private fun Fragment.requireViewOrThrow(): View {
    return runCatching { requireView() }.mapFailure { it.asNablaInternal() }.getOrThrow()
}

@NablaInternal
public fun <ViewBindingT : ViewBinding> Activity.viewBinding(
    bindingInflater: (LayoutInflater) -> ViewBindingT
): Lazy<ViewBindingT> = lazy { bindingInflater(layoutInflater) }

@NablaInternal
public val ViewBinding.context: Context
    get(): Context = root.context
