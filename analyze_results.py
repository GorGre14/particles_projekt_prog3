#!/usr/bin/env python3
"""
Performance Analysis and Visualization Script
Analyzes performance test results and generates comprehensive charts
"""

import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
import numpy as np
import os
from pathlib import Path

# Set style for professional charts
plt.style.use('seaborn-v0_8-darkgrid')
sns.set_palette("husl")

class PerformanceAnalyzer:
    def __init__(self):
        self.particle_data = None
        self.cycle_data = None
        
    def load_data(self):
        """Load performance test results"""
        try:
            if os.path.exists('particle_scaling_results.csv'):
                self.particle_data = pd.read_csv('particle_scaling_results.csv')
                print(f"Loaded particle scaling data: {len(self.particle_data)} records")
            else:
                print("Warning: particle_scaling_results.csv not found")
                
            if os.path.exists('cycle_scaling_results.csv'):
                self.cycle_data = pd.read_csv('cycle_scaling_results.csv')
                print(f"Loaded cycle scaling data: {len(self.cycle_data)} records")
            else:
                print("Warning: cycle_scaling_results.csv not found")
                
        except Exception as e:
            print(f"Error loading data: {e}")
            
    def create_chart1_particle_scaling(self):
        """Chart 1: Particle Scaling Performance (Runtime vs Cycles)"""
        if self.particle_data is None:
            print("No particle scaling data available")
            return
            
        plt.figure(figsize=(12, 8))
        
        for mode in self.particle_data['mode'].unique():
            mode_data = self.particle_data[self.particle_data['mode'] == mode]
            plt.plot(mode_data['cycles'], mode_data['average_time'], 
                    marker='o', linewidth=2, markersize=8, label=mode.title())
            
            # Add error bars if std_dev is available
            if 'std_dev' in mode_data.columns:
                plt.errorbar(mode_data['cycles'], mode_data['average_time'], 
                           yerr=mode_data['std_dev'], alpha=0.3, capsize=5)
        
        plt.xlabel('Number of Cycles', fontsize=12)
        plt.ylabel('Runtime (seconds)', fontsize=12)
        plt.title('Performance Scaling: Runtime vs Number of Cycles\n(Fixed Particles)', fontsize=14, fontweight='bold')
        plt.legend(fontsize=11)
        plt.grid(True, alpha=0.3)
        
        # Add annotations
        particles = self.particle_data['particles'].iloc[0]
        plt.text(0.02, 0.98, f'Fixed: {particles} particles', transform=plt.gca().transAxes,
                verticalalignment='top', bbox=dict(boxstyle='round', facecolor='wheat', alpha=0.8))
        
        plt.tight_layout()
        plt.savefig('chart1_particle_scaling.png', dpi=300, bbox_inches='tight')
        plt.show()
        
    def create_chart2_cycle_scaling(self):
        """Chart 2: Cycle Scaling Performance (Runtime vs Particles)"""
        if self.cycle_data is None:
            print("No cycle scaling data available")
            return
            
        plt.figure(figsize=(12, 8))
        
        for mode in self.cycle_data['mode'].unique():
            mode_data = self.cycle_data[self.cycle_data['mode'] == mode]
            plt.plot(mode_data['particles'], mode_data['average_time'], 
                    marker='s', linewidth=2, markersize=8, label=mode.title())
            
            # Add error bars if std_dev is available
            if 'std_dev' in mode_data.columns:
                plt.errorbar(mode_data['particles'], mode_data['average_time'], 
                           yerr=mode_data['std_dev'], alpha=0.3, capsize=5)
        
        plt.xlabel('Number of Particles', fontsize=12)
        plt.ylabel('Runtime (seconds)', fontsize=12)
        plt.title('Performance Scaling: Runtime vs Number of Particles\n(Fixed Cycles)', fontsize=14, fontweight='bold')
        plt.legend(fontsize=11)
        plt.grid(True, alpha=0.3)
        
        # Add annotations
        cycles = self.cycle_data['cycles'].iloc[0]
        plt.text(0.02, 0.98, f'Fixed: {cycles} cycles', transform=plt.gca().transAxes,
                verticalalignment='top', bbox=dict(boxstyle='round', facecolor='wheat', alpha=0.8))
        
        plt.tight_layout()
        plt.savefig('chart2_cycle_scaling.png', dpi=300, bbox_inches='tight')
        plt.show()
        
    def create_chart3_speedup_analysis(self):
        """Chart 3: Speedup Analysis"""
        plt.figure(figsize=(12, 8))
        
        # Combine both datasets for comprehensive analysis
        all_data = []
        
        if self.particle_data is not None:
            for _, row in self.particle_data.iterrows():
                problem_size = row['particles'] * row['cycles']
                all_data.append({
                    'problem_size': problem_size,
                    'mode': row['mode'],
                    'runtime': row['average_time'],
                    'test_type': 'particle_scaling'
                })
                
        if self.cycle_data is not None:
            for _, row in self.cycle_data.iterrows():
                problem_size = row['particles'] * row['cycles']
                all_data.append({
                    'problem_size': problem_size,
                    'mode': row['mode'],
                    'runtime': row['average_time'],
                    'test_type': 'cycle_scaling'
                })
        
        if not all_data:
            print("No data available for speedup analysis")
            return
            
        df = pd.DataFrame(all_data)
        
        # Calculate speedups relative to sequential
        problem_sizes = sorted(df['problem_size'].unique())
        
        parallel_speedups = []
        distributed_speedups = []
        sizes_with_data = []
        
        for size in problem_sizes:
            size_data = df[df['problem_size'] == size]
            
            sequential_time = size_data[size_data['mode'] == 'sequential']['runtime'].mean()
            parallel_time = size_data[size_data['mode'] == 'parallel']['runtime'].mean()
            distributed_time = size_data[size_data['mode'] == 'distributed']['runtime'].mean()
            
            if pd.notna(sequential_time) and pd.notna(parallel_time) and pd.notna(distributed_time):
                parallel_speedups.append(sequential_time / parallel_time)
                distributed_speedups.append(sequential_time / distributed_time)
                sizes_with_data.append(size)
        
        plt.plot(sizes_with_data, parallel_speedups, marker='^', linewidth=2, 
                markersize=8, label='Parallel Speedup', color='green')
        plt.plot(sizes_with_data, distributed_speedups, marker='d', linewidth=2, 
                markersize=8, label='Distributed Speedup', color='red')
        
        # Add horizontal line at speedup = 1
        plt.axhline(y=1, color='black', linestyle='--', alpha=0.5, label='No Speedup')
        
        plt.xlabel('Problem Size (Particles × Cycles)', fontsize=12)
        plt.ylabel('Speedup Ratio', fontsize=12)
        plt.title('Speedup Analysis: Parallel and Distributed vs Sequential', fontsize=14, fontweight='bold')
        plt.legend(fontsize=11)
        plt.grid(True, alpha=0.3)
        
        # Format x-axis for readability
        plt.ticklabel_format(style='scientific', axis='x', scilimits=(0,0))
        
        plt.tight_layout()
        plt.savefig('chart3_speedup_analysis.png', dpi=300, bbox_inches='tight')
        plt.show()
        
    def create_chart4_efficiency_analysis(self):
        """Chart 4: Efficiency Analysis"""
        fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(15, 6))
        
        # Left plot: Efficiency by Problem Size Categories
        if self.cycle_data is not None:
            # Categorize problem sizes
            self.cycle_data['size_category'] = pd.cut(
                self.cycle_data['particles'] * self.cycle_data['cycles'],
                bins=3, 
                labels=['Small', 'Medium', 'Large']
            )
            
            # Calculate efficiency (sequential_time / mode_time)
            efficiency_data = []
            
            for category in ['Small', 'Medium', 'Large']:
                cat_data = self.cycle_data[self.cycle_data['size_category'] == category]
                
                if len(cat_data) > 0:
                    seq_time = cat_data[cat_data['mode'] == 'sequential']['average_time'].mean()
                    par_time = cat_data[cat_data['mode'] == 'parallel']['average_time'].mean()
                    dist_time = cat_data[cat_data['mode'] == 'distributed']['average_time'].mean()
                    
                    if pd.notna(seq_time) and pd.notna(par_time):
                        efficiency_data.append({
                            'Category': category,
                            'Mode': 'Parallel',
                            'Efficiency': (seq_time / par_time) * 100
                        })
                    
                    if pd.notna(seq_time) and pd.notna(dist_time):
                        efficiency_data.append({
                            'Category': category,
                            'Mode': 'Distributed', 
                            'Efficiency': (seq_time / dist_time) * 100
                        })
            
            if efficiency_data:
                eff_df = pd.DataFrame(efficiency_data)
                sns.barplot(data=eff_df, x='Category', y='Efficiency', hue='Mode', ax=ax1)
                ax1.axhline(y=100, color='black', linestyle='--', alpha=0.5)
                ax1.set_title('Efficiency by Problem Size Category')
                ax1.set_ylabel('Efficiency (%)')
                ax1.legend()
        
        # Right plot: Runtime Distribution by Mode
        if self.cycle_data is not None:
            modes_data = []
            for mode in self.cycle_data['mode'].unique():
                mode_data = self.cycle_data[self.cycle_data['mode'] == mode]['average_time']
                modes_data.extend([(time, mode.title()) for time in mode_data])
            
            if modes_data:
                runtime_df = pd.DataFrame(modes_data, columns=['Runtime', 'Mode'])
                sns.boxplot(data=runtime_df, x='Mode', y='Runtime', ax=ax2)
                ax2.set_title('Runtime Distribution by Mode')
                ax2.set_ylabel('Runtime (seconds)')
        
        plt.tight_layout()
        plt.savefig('chart4_efficiency_analysis.png', dpi=300, bbox_inches='tight')
        plt.show()
        
    def generate_summary_statistics(self):
        """Generate summary statistics for the report"""
        print("\n" + "="*60)
        print("PERFORMANCE ANALYSIS SUMMARY")
        print("="*60)
        
        if self.particle_data is not None:
            print("\nParticle Scaling Test Results:")
            print(f"Fixed particles: {self.particle_data['particles'].iloc[0]}")
            print(f"Cycle range: {self.particle_data['cycles'].min()} - {self.particle_data['cycles'].max()}")
            
            for mode in self.particle_data['mode'].unique():
                mode_data = self.particle_data[self.particle_data['mode'] == mode]
                avg_time = mode_data['average_time'].mean()
                print(f"  {mode.title()} average runtime: {avg_time:.3f}s")
        
        if self.cycle_data is not None:
            print(f"\nCycle Scaling Test Results:")
            print(f"Fixed cycles: {self.cycle_data['cycles'].iloc[0]}")
            print(f"Particle range: {self.cycle_data['particles'].min()} - {self.cycle_data['particles'].max()}")
            
            for mode in self.cycle_data['mode'].unique():
                mode_data = self.cycle_data[self.cycle_data['mode'] == mode]
                avg_time = mode_data['average_time'].mean()
                print(f"  {mode.title()} average runtime: {avg_time:.3f}s")
        
        # Calculate overall speedups
        if self.cycle_data is not None and len(self.cycle_data) > 0:
            seq_avg = self.cycle_data[self.cycle_data['mode'] == 'sequential']['average_time'].mean()
            par_avg = self.cycle_data[self.cycle_data['mode'] == 'parallel']['average_time'].mean()
            dist_avg = self.cycle_data[self.cycle_data['mode'] == 'distributed']['average_time'].mean()
            
            print(f"\nOverall Average Speedups:")
            if pd.notna(seq_avg) and pd.notna(par_avg):
                print(f"  Parallel: {seq_avg/par_avg:.2f}x")
            if pd.notna(seq_avg) and pd.notna(dist_avg):
                print(f"  Distributed: {seq_avg/dist_avg:.2f}x")
        
    def run_analysis(self):
        """Run complete analysis suite"""
        print("ChargedParticles Performance Analysis")
        print("=====================================")
        
        self.load_data()
        
        if self.particle_data is not None or self.cycle_data is not None:
            print("\nGenerating performance charts...")
            
            if self.particle_data is not None:
                self.create_chart1_particle_scaling()
                
            if self.cycle_data is not None:
                self.create_chart2_cycle_scaling()
                
            self.create_chart3_speedup_analysis()
            self.create_chart4_efficiency_analysis()
            
            self.generate_summary_statistics()
            
            print(f"\nAnalysis complete! Charts saved:")
            print("- chart1_particle_scaling.png")
            print("- chart2_cycle_scaling.png") 
            print("- chart3_speedup_analysis.png")
            print("- chart4_efficiency_analysis.png")
        else:
            print("No data files found. Please run performance tests first.")

def main():
    analyzer = PerformanceAnalyzer()
    analyzer.run_analysis()

if __name__ == "__main__":
    main()