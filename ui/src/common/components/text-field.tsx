import type { ComponentPropsWithRef, CSSProperties } from 'react';
import './text-field.css';

export interface TextFieldProps extends Omit<ComponentPropsWithRef<'input'>, 'width'> {
    /** Override the default behavior of filling the available horizontal space. */
    width?: CSSProperties['width'];
}

export const TextField = ({
    className,
    type = 'text',
    width,
    style,
    ...props
}: TextFieldProps) => (
    <input
        {...props}
        type={type}
        className={['text-field', className].filter(Boolean).join(' ')}
        data-auto-width={(width === undefined && style?.width === undefined) || undefined}
        style={width === undefined ? style : { ...style, width }}
    />
);
