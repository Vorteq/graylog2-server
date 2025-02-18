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
import { useState } from 'react';

import type { DataNode } from 'preflight/types';
import { ConfirmDialog } from 'components/common';
import { MenuItem } from 'components/bootstrap';
import OverlayDropdownButton from 'components/common/OverlayDropdownButton';
import { MORE_ACTIONS_TITLE, MORE_ACTIONS_HOVER_TITLE } from 'components/common/EntityDataTable/Constants';

import {
  rejoinDataNode,
  removeDataNode,
  renewDatanodeCertificate,
  stopDataNode,
  startDataNode,
} from '../hooks/useDataNodes';

type Props = {
  dataNode: DataNode,
};

const DIALOG_TYPES = {
  STOP: 'stop',
  REJOIN: 'rejoin',
  REMOVE: 'remove',
  RENEW_CERT: 'renew',
};

const DIALOG_TEXT = {
  [DIALOG_TYPES.REJOIN]: {
    dialogTitle: 'Rejoin Data Node',
    dialogBody: (datanode: string) => `Are you sure you want to rejoin Data Node "${datanode}"?`,
  },
  [DIALOG_TYPES.REMOVE]: {
    dialogTitle: 'Remove Data Node',
    dialogBody: (datanode: string) => `Are you sure you want to remove Data Node "${datanode}"?`,
  },
  [DIALOG_TYPES.STOP]: {
    dialogTitle: 'Stop Data Node',
    dialogBody: (datanode: string) => `Are you sure you want to stop Data Node "${datanode}"?`,
  },
};

const DataNodeActions = ({ dataNode }: Props) => {
  const [showDialog, setShowDialog] = useState(false);
  const [dialogType, setDialogType] = useState(null);

  const updateState = ({ show, type }) => {
    setShowDialog(show);
    setDialogType(type);
  };

  const handleAction = (action) => {
    switch (action) {
      case DIALOG_TYPES.REJOIN:
        updateState({ show: true, type: DIALOG_TYPES.REJOIN });

        break;
      case DIALOG_TYPES.REMOVE:
        updateState({ show: true, type: DIALOG_TYPES.REMOVE });

        break;
      case DIALOG_TYPES.STOP:
        updateState({ show: true, type: DIALOG_TYPES.STOP });

        break;
      default:
        break;
    }
  };

  const handleClearState = () => {
    updateState({ show: false, type: null });
  };

  const handleConfirm = () => {
    switch (dialogType) {
      case 'rejoin':
        rejoinDataNode(dataNode.node_id).then(() => {
          handleClearState();
        });

        break;
      case 'remove':
        removeDataNode(dataNode.node_id).then(() => {
          handleClearState();
        });

        break;
      case 'stop':
        stopDataNode(dataNode.node_id).then(() => {
          handleClearState();
        });

        break;
      default:
        break;
    }
  };

  const isDatanodeRunning = dataNode.data_node_status === 'AVAILABLE';
  const isDatanodeRemoved = dataNode.data_node_status === 'REMOVED';
  const isRemovingDatanode = dataNode.data_node_status === 'REMOVING';

  return (
    <>
      <OverlayDropdownButton title={MORE_ACTIONS_TITLE}
                             bsSize="xsmall"
                             buttonTitle={MORE_ACTIONS_HOVER_TITLE}
                             disabled={false}
                             dropdownZIndex={1000}>
        <MenuItem onSelect={() => renewDatanodeCertificate(dataNode.node_id)}>Renew certificate</MenuItem>
        {!isDatanodeRunning && <MenuItem onSelect={() => startDataNode(dataNode.node_id)}>Start</MenuItem>}
        {isDatanodeRunning && <MenuItem onSelect={() => handleAction(DIALOG_TYPES.STOP)}>Stop</MenuItem>}
        {isDatanodeRemoved && <MenuItem onSelect={() => handleAction(DIALOG_TYPES.REJOIN)}>Rejoin</MenuItem>}
        {(!isDatanodeRemoved || isRemovingDatanode) && <MenuItem onSelect={() => handleAction(DIALOG_TYPES.REMOVE)}>Remove</MenuItem>}
      </OverlayDropdownButton>
      {showDialog && (
        <ConfirmDialog title={DIALOG_TEXT[dialogType].dialogTitle}
                       show
                       onConfirm={handleConfirm}
                       onCancel={handleClearState}>
          {DIALOG_TEXT[dialogType].dialogBody(dataNode.hostname)}
        </ConfirmDialog>
      )}
    </>
  );
};

export default DataNodeActions;
