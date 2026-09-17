import './autocomplete.css';
import {
    type ChangeEventHandler,
    cloneElement,
    type CSSProperties,
    type ReactElement,
    useEffect,
    useMemo,
    useRef,
    useState,
} from 'react';
import {debounce} from '../utils.ts';

type InputChangeProps = {
    value?: string;
    onChange?: ChangeEventHandler<HTMLInputElement>;
};

export interface AutocompleteOption {
    label: string;
    value: string;
}

export interface AutocompleteProps {
    inputComponent: ReactElement<InputChangeProps>;
    loadOptions: (query: string) => Promise<AutocompleteOption[]>;
    /** Keeps a parent-controlled input value in sync with typing and selection. */
    onInputValueChange?: (value: string) => void;
    style?: CSSProperties;
}

export const Autocomplete = ({ inputComponent, loadOptions, onInputValueChange, style }: AutocompleteProps) => {
    const [inputValue, setInputValue] = useState('');
    const [isOpen, setIsOpen] = useState(false);
    const [isLoading, setIsLoading] = useState(false);
    const [options, setOptions] = useState<AutocompleteOption[]>([]);
    const containerRef = useRef<HTMLDivElement>(null);

    const updateInputValue = (value: string) => {
        setInputValue(value);
        onInputValueChange?.(value);
    };

    useEffect(() => {
        if (!isOpen) return;

        const closeOnOutsideClick = (event: PointerEvent) => {
            if (!containerRef.current?.contains(event.target as Node)) setIsOpen(false);
        };
        document.addEventListener('pointerdown', closeOnOutsideClick);
        return () => document.removeEventListener('pointerdown', closeOnOutsideClick);
    }, [isOpen]);

    const loadSuggestions = useMemo(() => debounce(300, async (query: string) => {
        try {
            setOptions(query.trim() ? await loadOptions(query) : []);
        } catch {
            setOptions([]);
        } finally {
            setIsLoading(false);
        }
    }), [loadOptions]);

    return (
        <div ref={containerRef} className="autocomplete-input-container" style={style}>
            {cloneElement(inputComponent, {
                value: inputComponent.props.value ?? inputValue,
                onChange: (event) => {
                    const query = event.currentTarget.value;
                    inputComponent.props.onChange?.(event);
                    updateInputValue(query);
                    setIsOpen(Boolean(query.trim()));
                    setIsLoading(Boolean(query.trim()));
                    loadSuggestions(query);
                },
            })}
            {isOpen && (isLoading || options.length > 0) && (
                <div className="autocomplete-container">
                    {isLoading ? <div role="status">Loading...</div> : options.map((option, index) => (
                        <button
                            key={`${option.value}-${index}`}
                            type="button"
                            className="autocomplete-option"
                            onClick={() => {
                                updateInputValue(option.label);
                                setIsOpen(false);
                            }}
                        >
                            {option.label}
                        </button>
                    ))}
                </div>
            )}
        </div>
    );
};
