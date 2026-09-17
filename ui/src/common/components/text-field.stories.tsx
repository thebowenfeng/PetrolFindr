import type { Meta, StoryObj } from '@storybook/react-vite';
import { useState } from 'react';
import { Dropdown } from './dropdown';
import { TextField, type TextFieldProps } from './text-field';

const ControlledTextField = (props: TextFieldProps) => {
    const [value, setValue] = useState(props.value ?? '');

    return (
        <TextField
            {...props}
            value={value}
            onChange={(event) => {
                setValue(event.target.value);
                props.onChange?.(event);
            }}
        />
    );
};

const meta = {
    title: 'Common/TextField',
    component: TextField,
    parameters: {
        layout: 'padded',
    },
    argTypes: {
        width: { control: 'text' },
    },
    args: {
        placeholder: 'Suburb or postcode',
        'aria-label': 'Location search',
    },
} satisfies Meta<typeof TextField>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {};

export const Controlled: Story = {
    args: {
        value: 'Richmond',
    },
    render: (args) => <ControlledTextField {...args} />,
};

export const Disabled: Story = {
    args: {
        disabled: true,
    },
};

export const CustomWidth: Story = {
    args: {
        width: 'min(80vw, 24rem)',
    },
};

export const WithFilters: Story = {
    render: (args) => (
        <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: '0.375rem' }}>
            <Dropdown
                options={[
                    { value: 'GPS', label: 'Near me' },
                    { value: 'CUSTOM_GPS', label: 'Map pin' },
                ]}
                placeholder="Location"
                ariaLabel="Location filter"
            />
            <TextField {...args} placeholder="Enter an address" />
            <Dropdown
                options={[
                    { value: 'E10', label: 'E10' },
                    { value: 'BIODIESEL', label: 'BIODIESEL' },
                ]}
                placeholder="Fuel Type"
                ariaLabel="Fuel type"
            />
        </div>
    ),
};
