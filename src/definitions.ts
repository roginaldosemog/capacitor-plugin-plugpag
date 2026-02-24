export interface PlugPagPluginPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
}
