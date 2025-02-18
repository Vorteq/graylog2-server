/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import * as React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import type { IconName } from 'components/common/Icon';

import Icon from './Icon';
import Delayed from './Delayed';

const StyledIcon = styled(Icon)<{ $displayMargin: boolean }>(({ $displayMargin }) => (
  $displayMargin ? 'margin-right: 6px;' : ''
));

type Props = {
  delay?: number,
  name?: IconName,
  text?: string,
};

/**
 * Simple spinner to use while waiting for something to load.
 */
const Spinner = ({ name, text, delay, ...rest }: Props) => (
  <Delayed delay={delay}>
    <StyledIcon {...rest} name={name} $displayMargin={!!text?.trim()} spin />{text}
  </Delayed>
);

Spinner.propTypes = {
  /** Delay in ms before displaying the spinner */
  delay: PropTypes.number,
  /** Name of the Icon to use. */
  name: PropTypes.string,
  /** Text to show while loading. */
  text: PropTypes.string,
};

Spinner.defaultProps = {
  name: 'spinner',
  text: 'Loading...',
  delay: 200,
};

export default Spinner;
