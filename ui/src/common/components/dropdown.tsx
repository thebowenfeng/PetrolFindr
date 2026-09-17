import {
    type CSSProperties,
    type FocusEvent,
    type ReactNode,
    useState,
} from 'react';
import './dropdown.css';

export interface DropdownOption {
    value: string;
    label: ReactNode;
    disabled?: boolean;
}

export interface DropdownProps {
    options: readonly DropdownOption[];
    value?: string;
    onValueChange?: (value: string, option: DropdownOption) => void;
    placeholder?: ReactNode;
    ariaLabel?: string;
    className?: string;
    /** Explicit widths take precedence over sizing to the options. */
    width?: CSSProperties['width'];
    /** Size to the widest option or placeholder. Defaults to true. */
    sizeToOptions?: boolean;
    disabled?: boolean;
    id?: string;
}

export const Dropdown = ({
    options,
    value,
    onValueChange,
    placeholder = 'Select an option',
    ariaLabel = 'Select an option',
    className,
    width,
    sizeToOptions = true,
    disabled = false,
    id,
}: DropdownProps) => {
    const [isOpen, setIsOpen] = useState(false);
    const selectedOption = options.find((option) => option.value === value);
    const shouldSizeToOptions = sizeToOptions && width === undefined;

    const selectOption = (option: DropdownOption) => {
        onValueChange?.(option.value, option);
        setIsOpen(false);
    };

    const closeOnBlur = (event: FocusEvent<HTMLDivElement>) => {
        if (!event.currentTarget.contains(event.relatedTarget)) {
            setIsOpen(false);
        }
    };

    return (
        <div
            className={['dropdown', className].filter(Boolean).join(' ')}
            style={{ width, minWidth: width === undefined ? undefined : 0 }}
            data-size-to-options={shouldSizeToOptions || undefined}
            data-open={isOpen || undefined}
            onBlur={closeOnBlur}
        >
            <button
                type="button"
                id={id}
                className="dropdown__trigger"
                aria-label={ariaLabel}
                aria-haspopup="listbox"
                aria-expanded={isOpen}
                disabled={disabled}
                onClick={() => setIsOpen((open) => !open)}
            >
                <span className="dropdown__label">
                    <span className={selectedOption ? 'dropdown__value' : 'dropdown__placeholder'}>
                        {selectedOption?.label ?? placeholder}
                    </span>
                    {shouldSizeToOptions && (
                        <>
                            <span className="dropdown__placeholder dropdown__sizer" aria-hidden="true" inert>
                                {placeholder}
                            </span>
                            {options.map((option) => (
                                <span key={option.value} className="dropdown__value dropdown__sizer" aria-hidden="true" inert>
                                    {option.label}
                                </span>
                            ))}
                        </>
                    )}
                </span>
                <svg className="dropdown__chevron" viewBox="0 0 16 16" aria-hidden="true">
                    <path d="m4 6 4 4 4-4" />
                </svg>
            </button>

            {isOpen && (
                <ul className="dropdown__menu" role="listbox" aria-label={ariaLabel}>
                    {options.map((option) => (
                        <li key={option.value}>
                            <button
                                type="button"
                                className="dropdown__option"
                                role="option"
                                aria-selected={option.value === value}
                                disabled={option.disabled}
                                onClick={() => selectOption(option)}
                            >
                                <span>{option.label}</span>
                                {option.value === value && (
                                    <svg viewBox="0 0 16 16" aria-hidden="true">
                                        <path d="m3.5 8.5 3 3 6-7" />
                                    </svg>
                                )}
                            </button>
                        </li>
                    ))}
                </ul>
            )}
        </div>
    );
};
