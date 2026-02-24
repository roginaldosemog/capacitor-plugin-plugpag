import { WebPlugin } from '@capacitor/core';

import type { PlugPagPluginPlugin } from './definitions';

export class PlugPagPluginWeb extends WebPlugin implements PlugPagPluginPlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
